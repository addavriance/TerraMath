package me.adda.terramath.gui.preview;

import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.config.NoiseType;
import me.adda.terramath.math.parser.FormulaParser;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Solid/air grid of the previewed terrain, one bit per voxel (ix * GY * gz + iy * gz + iz).
 */
public final class VoxelField {
    public static final int VOXEL_SIZE_XZ = 2;
    public static final int GY = 192;
    public static final float WY_MIN = -64f;
    public static final float WY_MAX = 320f;

    // Outer compute runs on its own thread and waits on the pool, so the pool needs a spare thread
    private static final ExecutorService POOL = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()), runnable -> {
                Thread thread = new Thread(runnable, "TerraMath-Preview");
                thread.setDaemon(true);
                return thread;
            });

    /** Everything the grid depends on, equal params mean an equal grid. */
    public record Params(String formula, NoiseType noiseType,
                         double noiseScaleX, double noiseScaleY, double noiseScaleZ, double noiseHeightScale,
                         double coordScale, boolean densityMode,
                         double baseHeight, double heightVariation, double smoothing,
                         int halfWidth) {

        public static Params of(String formula, TerrainSettingsManager s, int halfWidth) {
            return new Params(formula, s.getNoiseType(),
                    s.getNoiseScaleX(), s.getNoiseScaleY(), s.getNoiseScaleZ(), s.getNoiseHeightScale(),
                    s.getCoordinateScale(), s.isUseDensityMode(),
                    s.getBaseHeight(), s.getHeightVariation(), s.getSmoothingFactor(),
                    halfWidth);
        }
    }

    public final long[] bits;
    public final int gx, gz;
    public final int halfWidth;
    public final String error;

    private VoxelField(long[] bits, int gx, int gz, int halfWidth, String error) {
        this.bits = bits;
        this.gx = gx;
        this.gz = gz;
        this.halfWidth = halfWidth;
        this.error = error;
    }

    private static int gridSize(int halfWidth) {
        return Math.max(8, halfWidth * 2 / VOXEL_SIZE_XZ);
    }

    private static VoxelField failed(Params p, String error) {
        int gx = gridSize(p.halfWidth());
        return new VoxelField(new long[bitsetSize(gx * GY * gx)], gx, gx, p.halfWidth(), error);
    }

    /** Размер bitset в long[] для n бит */
    private static int bitsetSize(int n) { return (n + 63) >>> 6; }

    /** Проверить бит [ix][iy][iz] */
    public static boolean bitGet(long[] bits, int sX, int sY, int ix, int iy, int iz) {
        int idx = ix * sX + iy * sY + iz;
        return (bits[idx >>> 6] & (1L << (idx & 63))) != 0;
    }

    public static VoxelField compute(Params p) {
        final String fText = p.formula();
        final double cs = p.coordScale();
        final boolean dm = p.densityMode();
        final double bh = p.baseHeight();
        final double hv = p.heightVariation();
        final double sf = p.smoothing();
        final int hw = p.halfWidth();
        final int gx = gridSize(hw);
        final int gz = gx;

        if (fText.isEmpty())
            return failed(p, "No formula");

        FormulaParser.CompiledFormula compiled;
        try {
            compiled = parseFormulaWithSettings(p);
        } catch (Exception e) {
            return failed(p, "Formula error");
        }

        boolean eq = fText.contains("=");
        double vsx = (hw * 2.0) / gx;
        double vsy = (WY_MAX - WY_MIN) / GY;
        double vsz = (hw * 2.0) / gz;
        int sX = GY * gz, sY = gz;
        int total = gx * GY * gz;
        float[] raw = new float[total];

        // evaluate однопоточно (PerlinNoise не thread-safe)
        for (int ix = 0; ix < gx; ix++) {
            double wx = -hw + (ix + 0.5) * vsx;
            double fx = wx / cs;
            for (int iz = 0; iz < gz; iz++) {
                double wz = -hw + (iz + 0.5) * vsz;
                double fz = wz / cs;
                for (int iy = 0; iy < GY; iy++) {
                    double wy = WY_MIN + (iy + 0.5) * vsy;
                    float v;
                    try { v = (float) compiled.evaluate(fx, wy / cs, fz); }
                    catch (Exception e) { v = 0f; }
                    raw[ix * sX + iy * sY + iz] = v;
                }
            }
        }

        // blur параллельно (только float[], thread-safe)
        float[] processed = raw;
        if (sf > 0) {
            int rXZ = Math.max(1, (int) Math.round(sf * cs / vsx));
            int rY = eq ? Math.max(1, (int) Math.round(sf * cs / vsy)) : 0;
            processed = separableBoxBlur(raw, gx, GY, gz, sX, sY, rXZ, rY);
        }

        // classify -> bitset (параллельно)
        final float[] finalRaw = processed;
        long[] result = new long[bitsetSize(total)];
        int nThreads = Runtime.getRuntime().availableProcessors();
        runParallel(nThreads, gx, ix -> {
            for (int iz = 0; iz < gz; iz++) {
                for (int iy = 0; iy < GY; iy++) {
                    double wy = WY_MIN + (iy + 0.5) * vsy;
                    if (toDensity(finalRaw[ix * sX + iy * sY + iz],
                            wy / cs, cs, dm, eq, bh, hv) > 0) {
                        int flatIdx = ix * sX + iy * sY + iz;
                        int word = flatIdx >>> 6;
                        long bit = 1L << (flatIdx & 63);
                        // Разные ix -> разные word при sX=GY*gz >= 64 -> нет гонок
                        // (при gx≥8 и GY=192 это всегда выполняется)
                        result[word] |= bit;
                    }
                }
            }
        });

        return new VoxelField(result, gx, gz, hw, null);
    }

    private static FormulaParser.CompiledFormula parseFormulaWithSettings(Params p) throws Exception {
        TerrainSettingsManager gtsm = TerrainSettingsManager.getInstance();
        NoiseType savedNt = gtsm.getNoiseType();
        double savedNsx = gtsm.getNoiseScaleX(), savedNsy = gtsm.getNoiseScaleY();
        double savedNsz = gtsm.getNoiseScaleZ(), savedNhs = gtsm.getNoiseHeightScale();
        gtsm.setNoiseType(p.noiseType() != null ? p.noiseType() : NoiseType.NONE);
        gtsm.setNoiseScaleX(p.noiseScaleX()); gtsm.setNoiseScaleY(p.noiseScaleY());
        gtsm.setNoiseScaleZ(p.noiseScaleZ()); gtsm.setNoiseHeightScale(p.noiseHeightScale());
        try {
            return FormulaParser.parse(p.formula());
        } finally {
            gtsm.setNoiseType(savedNt);
            gtsm.setNoiseScaleX(savedNsx); gtsm.setNoiseScaleY(savedNsy);
            gtsm.setNoiseScaleZ(savedNsz); gtsm.setNoiseHeightScale(savedNhs);
        }
    }

    private static float[] separableBoxBlur(float[] src, int gx, int gy, int gz,
                                     int sX, int sY, int rXZ, int rY) {
        int nT = Runtime.getRuntime().availableProcessors();
        float[] tmp1 = new float[src.length];
        float[] tmp2 = new float[src.length];

        // X
        runParallel(nT, gy, iy -> {
            for (int iz = 0; iz < gz; iz++) {
                double sum = 0; int cnt = 0;
                for (int ix = 0; ix <= Math.min(rXZ, gx-1); ix++) { sum += src[ix*sX+iy*sY+iz]; cnt++; }
                for (int ix = 0; ix < gx; ix++) {
                    int a = ix+rXZ+1; if (a < gx) { sum += src[a*sX+iy*sY+iz]; cnt++; }
                    int r = ix-rXZ-1; if (r >= 0) { sum -= src[r*sX+iy*sY+iz]; cnt--; }
                    tmp1[ix*sX+iy*sY+iz] = (float)(sum/cnt);
                }
            }
        });
        // Z
        runParallel(nT, gy, iy -> {
            for (int ix = 0; ix < gx; ix++) {
                double sum = 0; int cnt = 0;
                for (int iz = 0; iz <= Math.min(rXZ, gz-1); iz++) { sum += tmp1[ix*sX+iy*sY+iz]; cnt++; }
                for (int iz = 0; iz < gz; iz++) {
                    int a = iz+rXZ+1; if (a < gz) { sum += tmp1[ix*sX+iy*sY+a]; cnt++; }
                    int r = iz-rXZ-1; if (r >= 0) { sum -= tmp1[ix*sX+iy*sY+r]; cnt--; }
                    tmp2[ix*sX+iy*sY+iz] = (float)(sum/cnt);
                }
            }
        });
        if (rY <= 0) return tmp2;

        float[] out = new float[src.length];
        // Y
        runParallel(nT, gx, ix -> {
            for (int iz = 0; iz < gz; iz++) {
                double sum = 0; int cnt = 0;
                for (int iy = 0; iy <= Math.min(rY, gy-1); iy++) { sum += tmp2[ix*sX+iy*sY+iz]; cnt++; }
                for (int iy = 0; iy < gy; iy++) {
                    int a = iy+rY+1; if (a < gy) { sum += tmp2[ix*sX+a*sY+iz]; cnt++; }
                    int r = iy-rY-1; if (r >= 0) { sum -= tmp2[ix*sX+r*sY+iz]; cnt--; }
                    out[ix*sX+iy*sY+iz] = (float)(sum/cnt);
                }
            }
        });
        return out;
    }

    @FunctionalInterface private interface RowTask { void run(int row); }

    private static void runParallel(int nThreads, int rowCount, RowTask task) {
        CountDownLatch latch = new CountDownLatch(nThreads);
        AtomicInteger cursor = new AtomicInteger(0);
        for (int ti = 0; ti < nThreads; ti++) {
            POOL.submit(() -> {
                try {
                    int row;
                    while ((row = cursor.getAndIncrement()) < rowCount) task.run(row);
                } finally { latch.countDown(); }
            });
        }
        try { latch.await(); } catch (InterruptedException ignored) {}
    }

    private static double toDensity(double fv, double fy, double scale,
                                    boolean dm, boolean eq,
                                    double baseHeight, double heightVariation) {
        if (eq) {
            double d = dm ? (fv - (baseHeight-64.0)/scale) / Math.max(1e-6, heightVariation) : fv;
            return d >= 0 ? 1.0 : -1.0;
        }
        double tgt = dm ? baseHeight/scale + fv*heightVariation : 64.0/scale + fv;
        return tgt - fy >= 0 ? 1.0 : -1.0;
    }
}
