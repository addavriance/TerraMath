package me.adda.terramath.gui;

import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.config.NoiseType;
import me.adda.terramath.math.parser.FormulaParser;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Inline 3-D voxel preview panel rendered inside the config screen. */
public class PreviewPanel {

    // ── Voxel grid resolution ───────────────────────────────────────────
    // blocks/voxel_Y = (WY_MAX-WY_MIN)/GY  → currently 128/80 = 1.6 blk/voxel
    private static final int GX = 48, GY = 80, GZ = 48;
    /** Shared pool for parallel voxel column computation. Work-stealing = auto-scales to cores. */
    private static final ExecutorService VOXEL_POOL = Executors.newWorkStealingPool();
    // ── Y world-range ───────────────────────────────────────────────────
    // Narrowed to 0..128 — doubles Y resolution while keeping sea-level (64)
    // and all normal terrain heights visible.
    private static final float WY_MIN = 0f, WY_MAX = 128f;
    private static final double CAM_DIST  = 450.0;
    private static final double CAM_SCALE = 2.8;
    private static final int MAX_STEPS = 200;
    private static final int FACE_X = 1, FACE_Y = 2, FACE_Z = 3;
    /** Height of the bottom controls strip (quality + width). */
    public  static final int CTRL_H = 26;

    private String formulaText = "";
    private final byte[] voxels = new byte[GX * GY * GZ];
    private int[] buffer = new int[0];
    private int lastBufW, lastBufH;
    private boolean voxelsDirty = true;
    private boolean bufferDirty = true;
    private String errorMessage = null;

    private float yaw = 45f, pitch = 28f;
    private double lastMX, lastMY;
    private boolean dragging = false;

    // UI controls
    public int pixelScale = 2;   // 1=best quality, 4=fastest
    public int halfWidth  = 200; // X/Z half-range in blocks

    // Live settings reference
    private TerrainSettingsManager displaySettings = null;

    // Snapshot to detect changes
    private double snapCoordScale = Double.NaN;
    private boolean snapDensityMode = false;
    private double snapBaseHeight = Double.NaN;
    private double snapHeightVariation = Double.NaN;
    private double snapSmoothing = Double.NaN;
    private NoiseType snapNoiseType = null;
    private double snapNoiseScaleX = Double.NaN, snapNoiseScaleY = Double.NaN;
    private double snapNoiseScaleZ = Double.NaN, snapNoiseHeightScale = Double.NaN;
    private int snapHalfWidth = -1;

    // Render-committed world bounds (updated when bg thread finishes)
    private int renderHalfWidth = 200;

    // Background computation — written by bg thread, committed by render thread
    private volatile boolean computing = false;
    private volatile byte[] pendingVoxels = null;
    private volatile String pendingError = null;
    private volatile int pendingHalfWidth = 200;

    public void setDisplaySettings(TerrainSettingsManager settings) {
        displaySettings = settings;
    }

    public void setFormula(String formula) {
        String trimmed = formula == null ? "" : formula.trim();
        if (!trimmed.equals(formulaText)) {
            formulaText = trimmed;
            voxelsDirty = true;
            bufferDirty = true;
        }
    }

    public void render(GuiGraphics graphics, int x, int y, int w, int h, Font font) {
        int viewH = h - CTRL_H;
        int bufW = Math.max(1, w / pixelScale);
        int bufH = Math.max(1, viewH / pixelScale);

        if (bufW != lastBufW || bufH != lastBufH) {
            buffer = new int[bufW * bufH];
            lastBufW = bufW;
            lastBufH = bufH;
            bufferDirty = true;
        }

        // Commit completed background computation (render thread only)
        if (pendingVoxels != null && !computing) {
            System.arraycopy(pendingVoxels, 0, voxels, 0, voxels.length);
            errorMessage = pendingError;
            renderHalfWidth = pendingHalfWidth;
            pendingVoxels = null;
            pendingError = null;
            bufferDirty = true;
        }

        // Detect settings changes
        if (!voxelsDirty && !computing && displaySettings != null && settingsChanged()) {
            voxelsDirty = true;
        }
        if (!voxelsDirty && !computing && halfWidth != snapHalfWidth) {
            voxelsDirty = true;
        }

        // Kick off background compute if needed
        if (voxelsDirty && !computing) {
            startComputeAsync();
        }

        if (errorMessage != null) {
            graphics.fill(x, y, x + w, y + viewH, 0xFF1A1A1A);
            graphics.drawCenteredString(font, errorMessage, x + w / 2, y + viewH / 2 - 4, 0xFF5555);
        } else {
            if (bufferDirty) rebuildBuffer(bufW, bufH);
            renderBuffer(graphics, x, y, bufW, bufH);
            drawScaleOverlay(graphics, font, x, y, w, viewH);
            if (computing) graphics.drawString(font, "...", x + w - 16, y + 3, 0x88FFFFFF, false);
            graphics.drawString(font, "drag to rotate", x + 3, y + viewH - 10, 0x88FFFFFF, false);
        }

        renderControls(graphics, font, x, y + viewH, w);
        graphics.renderOutline(x - 1, y - 1, w + 2, h + 2, 0xFF444444);
    }

    public boolean mouseClicked(double mx, double my, int x, int y, int w, int h, int button) {
        int viewH = h - CTRL_H;
        if (button == 0 && mx >= x && mx < x + w && my >= y + viewH && my < y + h) {
            return handleControlClick((int) mx, (int) my, x, y + viewH, w);
        }
        if (button == 0 && mx >= x && mx < x + w && my >= y && my < y + viewH) {
            dragging = true;
            lastMX = mx;
            lastMY = my;
            return true;
        }
        return false;
    }

    public void mouseReleased() { dragging = false; }

    public boolean mouseDragged(double mx, double my) {
        if (!dragging) return false;
        yaw   += (float) (mx - lastMX) * 0.5f;
        pitch  = (float) Math.max(-89, Math.min(89, pitch + (my - lastMY) * 0.4));
        lastMX = mx;
        lastMY = my;
        bufferDirty = true;
        return true;
    }

    // -----------------------------------------------------------------------
    // Controls strip
    // -----------------------------------------------------------------------

    private void renderControls(GuiGraphics graphics, Font font, int x, int y, int w) {
        graphics.fill(x, y, x + w, y + CTRL_H, 0xFF222222);
        int half = w / 2;
        renderControl(graphics, font, x,        y, half,     "Quality", pixelScale + "x",
                pixelScale > 1, pixelScale < 4);
        renderControl(graphics, font, x + half, y, w - half, "Width",   halfWidth + " blk",
                halfWidth > 50, halfWidth < 400);
    }

    private void renderControl(GuiGraphics graphics, Font font,
                               int x, int y, int w, String label, String value,
                               boolean canDec, boolean canInc) {
        int cy = y + CTRL_H / 2 - 4;
        String text = label + ": " + value;
        int cx = x + w / 2;
        graphics.drawString(font, canDec ? "\u25c4" : " ", x + 3, cy,
                canDec ? 0xFFCCCCCC : 0xFF555555, false);
        graphics.drawCenteredString(font, text, cx, cy, 0xFFFFFFFF);
        int arrowX = x + w - font.width("\u25ba") - 3;
        graphics.drawString(font, canInc ? "\u25ba" : " ", arrowX, cy,
                canInc ? 0xFFCCCCCC : 0xFF555555, false);
    }

    private boolean handleControlClick(int mx, int my, int x, int y, int w) {
        int half = w / 2;
        if (mx < x + half) {
            int cx = x + half / 2;
            if (mx < cx - 8) { if (pixelScale > 1) { pixelScale--; bufferDirty = true; } }
            else if (mx > cx + 8) { if (pixelScale < 4) { pixelScale++; bufferDirty = true; } }
        } else {
            int cx = x + half + (w - half) / 2;
            if (mx < cx - 8) { if (halfWidth > 50)  { halfWidth -= 50; voxelsDirty = true; } }
            else if (mx > cx + 8) { if (halfWidth < 400) { halfWidth += 50; voxelsDirty = true; } }
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Settings change detection
    // -----------------------------------------------------------------------

    private boolean settingsChanged() {
        return displaySettings.getCoordinateScale() != snapCoordScale
            || displaySettings.isUseDensityMode()   != snapDensityMode
            || displaySettings.getBaseHeight()       != snapBaseHeight
            || displaySettings.getHeightVariation()  != snapHeightVariation
            || displaySettings.getSmoothingFactor()  != snapSmoothing
            || displaySettings.getNoiseType()        != snapNoiseType
            || displaySettings.getNoiseScaleX()      != snapNoiseScaleX
            || displaySettings.getNoiseScaleY()      != snapNoiseScaleY
            || displaySettings.getNoiseScaleZ()      != snapNoiseScaleZ
            || displaySettings.getNoiseHeightScale() != snapNoiseHeightScale;
    }

    private void saveSnapshot(TerrainSettingsManager s) {
        snapCoordScale       = s.getCoordinateScale();
        snapDensityMode      = s.isUseDensityMode();
        snapBaseHeight       = s.getBaseHeight();
        snapHeightVariation  = s.getHeightVariation();
        snapSmoothing        = s.getSmoothingFactor();
        snapNoiseType        = s.getNoiseType();
        snapNoiseScaleX      = s.getNoiseScaleX();
        snapNoiseScaleY      = s.getNoiseScaleY();
        snapNoiseScaleZ      = s.getNoiseScaleZ();
        snapNoiseHeightScale = s.getNoiseHeightScale();
        snapHalfWidth        = halfWidth;
    }

    // -----------------------------------------------------------------------
    // Background voxel computation
    // -----------------------------------------------------------------------

    private void startComputeAsync() {
        voxelsDirty = false;
        computing   = true;

        TerrainSettingsManager src =
                displaySettings != null ? displaySettings
                        : TerrainSettingsManager.getInstance();
        saveSnapshot(src);

        final String  fText = formulaText;
        final double  cs    = snapCoordScale;
        final boolean dm    = snapDensityMode;
        final double  bh    = snapBaseHeight;
        final double  hv    = snapHeightVariation;
        final double  sf    = snapSmoothing;
        final NoiseType nt  = snapNoiseType;
        final double  nsx   = snapNoiseScaleX;
        final double  nsy   = snapNoiseScaleY;
        final double  nsz   = snapNoiseScaleZ;
        final double  nhs   = snapNoiseHeightScale;
        final int     hw    = snapHalfWidth;
        final double  wxMin = -hw, wxMax = hw;
        final double  wzMin = -hw, wzMax = hw;

        Thread t = new Thread(() -> {
            if (fText.isEmpty()) {
                pendingError     = "No formula";
                pendingVoxels    = new byte[GX * GY * GZ];
                pendingHalfWidth = hw;
                computing        = false;
                return;
            }

            // Temporarily apply displaySettings noise to the global TerrainSettingsManager so
            // FormulaFormatter.wrapWithNoise() embeds the correct noise into the compiled formula.
            TerrainSettingsManager gtsm = TerrainSettingsManager.getInstance();
            NoiseType savedNt = gtsm.getNoiseType();
            double savedNsx = gtsm.getNoiseScaleX(), savedNsy = gtsm.getNoiseScaleY();
            double savedNsz = gtsm.getNoiseScaleZ(), savedNhs = gtsm.getNoiseHeightScale();
            gtsm.setNoiseType(nt != null ? nt : NoiseType.NONE);
            gtsm.setNoiseScaleX(nsx); gtsm.setNoiseScaleY(nsy);
            gtsm.setNoiseScaleZ(nsz); gtsm.setNoiseHeightScale(nhs);

            FormulaParser.CompiledFormula compiled;
            try {
                compiled = FormulaParser.parse(fText);
            } catch (Exception e) {
                pendingError     = "Formula error";
                pendingVoxels    = new byte[GX * GY * GZ];
                pendingHalfWidth = hw;
                computing        = false;
                return;
            } finally {
                gtsm.setNoiseType(savedNt);
                gtsm.setNoiseScaleX(savedNsx); gtsm.setNoiseScaleY(savedNsy);
                gtsm.setNoiseScaleZ(savedNsz); gtsm.setNoiseHeightScale(savedNhs);
            }

            boolean eq  = fText.contains("=");
            double  vsx = (wxMax - wxMin) / GX;
            double  vsy = (WY_MAX - WY_MIN) / GY;
            double  vsz = (wzMax - wzMin) / GZ;
            byte[] result = new byte[GX * GY * GZ];

            // Warm up CompositeNoise lazy-init in this thread so parallel threads only do reads.
            try { compiled.evaluate(0, 0, 0); } catch (Exception ignored) {}

            int nThreads = Math.max(1, Runtime.getRuntime().availableProcessors());
            CountDownLatch latch = new CountDownLatch(nThreads);
            AtomicInteger nextIx = new AtomicInteger(0);
            for (int ti = 0; ti < nThreads; ti++) {
                VOXEL_POOL.submit(() -> {
                    try {
                        int ix;
                        while ((ix = nextIx.getAndIncrement()) < GX) {
                            double fx = (wxMin + (ix + 0.5) * vsx) / cs;
                            for (int iz = 0; iz < GZ; iz++) {
                                double fz = (wzMin + (iz + 0.5) * vsz) / cs;
                                for (int iy = 0; iy < GY; iy++) {
                                    double wy = WY_MIN + (iy + 0.5) * vsy;
                                    double fy = wy / cs;
                                    double density;
                                    try {
                                        double fv = compiled.evaluate(fx, fy, fz);
                                        density = toDensity(fv, fy, cs, dm, eq, bh, hv, sf);
                                    } catch (Exception e) { density = -1; }
                                    result[ix * GY * GZ + iy * GZ + iz] = density > 0 ? (byte) 1 : (byte) 0;
                                }
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            try { latch.await(); } catch (InterruptedException ignored) {}
            pendingError     = null;
            pendingVoxels    = result;
            pendingHalfWidth = hw;
            computing        = false;
        }, "terramath-preview");
        t.setDaemon(true);
        t.start();
    }

    private static double toDensity(double fv, double fy, double scale, boolean dm, boolean eq,
            double baseHeight, double heightVariation, double smoothing) {
        if (eq) return fv;
        if (!dm) return fy < 64.0 / scale + fv ? 1 : -1;
        double tgt  = baseHeight / scale + fv * heightVariation;
        double dist = Math.abs(fy - tgt);
        return fy < tgt ? 1 - (dist / tgt) * smoothing
                        : -((dist / (heightVariation * scale)) * smoothing);
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    private void drawScaleOverlay(GuiGraphics graphics, Font font, int x, int y, int w, int h) {
        int xzBlocks = renderHalfWidth * 2;
        int yLo = (int) WY_MIN, yHi = (int) WY_MAX;
        String label = "X/Z " + xzBlocks + " blk  Y " + yLo + ".." + yHi;
        graphics.drawString(font, label, x + 3, y + h - 20, 0x66FFFFFF, false);
    }

    private void rebuildBuffer(int bufW, int bufH) {
        double wxMin = -renderHalfWidth, wxMax = renderHalfWidth;
        double wzMin = -renderHalfWidth, wzMax = renderHalfWidth;

        double yr = Math.toRadians(yaw);
        double pr = Math.toRadians(pitch);

        double ldx =  Math.sin(yr) * Math.cos(pr);
        double ldy = -Math.sin(pr);
        double ldz =  Math.cos(yr) * Math.cos(pr);
        double rdx =  Math.cos(yr);
        double rdz = -Math.sin(yr);
        double udx =  Math.sin(yr) * Math.sin(pr);
        double udy =  Math.cos(pr);
        double udz =  Math.cos(yr) * Math.sin(pr);

        double cx = (wxMin + wxMax) * 0.5;
        double cy = (WY_MIN + WY_MAX) * 0.5;
        double cz = (wzMin + wzMax) * 0.5;
        double camX = cx - ldx * CAM_DIST;
        double camY = cy - ldy * CAM_DIST;
        double camZ = cz - ldz * CAM_DIST;

        double vsx = (wxMax - wxMin) / GX;
        double vsy = (WY_MAX - WY_MIN) / GY;
        double vsz = (wzMax - wzMin) / GZ;

        for (int py = 0; py < bufH; py++) {
            double sdY = -(py - bufH * 0.5) * CAM_SCALE;
            for (int px = 0; px < bufW; px++) {
                double sdX = (px - bufW * 0.5) * CAM_SCALE;
                double ox = camX + rdx * sdX + udx * sdY;
                double oy = camY +              udy * sdY;
                double oz = camZ + rdz * sdX + udz * sdY;
                buffer[py * bufW + px] = rayCast(ox, oy, oz, ldx, ldy, ldz,
                        vsx, vsy, vsz, wxMin, wxMax, wzMin, wzMax);
            }
        }
        bufferDirty = false;
    }

    private int rayCast(double ox, double oy, double oz,
                        double dx, double dy, double dz,
                        double vsx, double vsy, double vsz,
                        double wxMin, double wxMax, double wzMin, double wzMax) {
        // AABB slab intersection: skip empty space before the voxel grid
        double tNear = 0.0, tFar = Double.MAX_VALUE;
        if (Math.abs(dx) > 1e-12) {
            double t1 = (wxMin - ox) / dx, t2 = (wxMax - ox) / dx;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tNear = Math.max(tNear, t1); tFar = Math.min(tFar, t2);
        } else if (ox < wxMin || ox > wxMax) return 0xFF87CEEB;
        if (Math.abs(dy) > 1e-12) {
            double t1 = (WY_MIN - oy) / dy, t2 = (WY_MAX - oy) / dy;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tNear = Math.max(tNear, t1); tFar = Math.min(tFar, t2);
        } else if (oy < WY_MIN || oy > WY_MAX) return 0xFF87CEEB;
        if (Math.abs(dz) > 1e-12) {
            double t1 = (wzMin - oz) / dz, t2 = (wzMax - oz) / dz;
            if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
            tNear = Math.max(tNear, t1); tFar = Math.min(tFar, t2);
        } else if (oz < wzMin || oz > wzMax) return 0xFF87CEEB;
        if (tNear > tFar || tFar < 0) return 0xFF87CEEB;
        if (tNear > 0) { ox += dx * tNear; oy += dy * tNear; oz += dz * tNear; }

        double gx = (ox - wxMin) / vsx;
        double gy = (oy - WY_MIN) / vsy;
        double gz = (oz - wzMin) / vsz;
        int ix = (int) Math.floor(gx);
        int iy = (int) Math.floor(gy);
        int iz = (int) Math.floor(gz);
        int stepX = dx >= 0 ? 1 : -1;
        int stepY = dy >= 0 ? 1 : -1;
        int stepZ = dz >= 0 ? 1 : -1;
        double gdx = dx / vsx, gdy = dy / vsy, gdz = dz / vsz;
        double tMaxX = stepX > 0 ? (Math.floor(gx) + 1 - gx) / Math.abs(gdx) : (gx - Math.floor(gx)) / Math.abs(gdx);
        double tMaxY = stepY > 0 ? (Math.floor(gy) + 1 - gy) / Math.abs(gdy) : (gy - Math.floor(gy)) / Math.abs(gdy);
        double tMaxZ = stepZ > 0 ? (Math.floor(gz) + 1 - gz) / Math.abs(gdz) : (gz - Math.floor(gz)) / Math.abs(gdz);
        double tDX = 1.0 / Math.abs(gdx), tDY = 1.0 / Math.abs(gdy), tDZ = 1.0 / Math.abs(gdz);
        int face = 0;
        for (int s = 0; s < MAX_STEPS; s++) {
            if (ix >= 0 && ix < GX && iy >= 0 && iy < GY && iz >= 0 && iz < GZ) {
                if (voxels[ix * GY * GZ + iy * GZ + iz] != 0) return voxelColor(ix, iy, iz, face);
            } else {
                if ((ix < 0 && stepX < 0) || (ix >= GX && stepX > 0)) break;
                if ((iy < 0 && stepY < 0) || (iy >= GY && stepY > 0)) break;
                if ((iz < 0 && stepZ < 0) || (iz >= GZ && stepZ > 0)) break;
            }
            if (tMaxX <= tMaxY && tMaxX <= tMaxZ) { ix += stepX; tMaxX += tDX; face = FACE_X; }
            else if (tMaxY <= tMaxZ) { iy += stepY; tMaxY += tDY; face = FACE_Y; }
            else { iz += stepZ; tMaxZ += tDZ; face = FACE_Z; }
        }
        return 0xFF87CEEB;
    }

    private int voxelColor(int ix, int iy, int iz, int face) {
        double wy = WY_MIN + (iy + 0.5) * (WY_MAX - WY_MIN) / GY;
        boolean belowSea = wy < 63.0;
        boolean topOpen = (iy + 1 >= GY) || (voxels[ix * GY * GZ + (iy + 1) * GZ + iz] == 0);
        boolean isGrass = topOpen && !belowSea;
        if (isGrass) return switch (face) {
            case FACE_Y -> 0xFF5A9A3A;
            case FACE_X -> 0xFF4A8030;
            case FACE_Z -> 0xFF3A6020;
            default     -> 0xFF5A9A3A;
        };
        if (belowSea) return switch (face) {
            case FACE_Y -> 0xFF555555;
            case FACE_X -> 0xFF3A3A3A;
            case FACE_Z -> 0xFF252525;
            default     -> 0xFF555555;
        };
        return switch (face) {
            case FACE_Y -> 0xFF888888;
            case FACE_X -> 0xFF666666;
            case FACE_Z -> 0xFF444444;
            default     -> 0xFF888888;
        };
    }

    private void renderBuffer(GuiGraphics graphics, int x, int y, int bufW, int bufH) {
        for (int py = 0; py < bufH; py++) {
            int y0 = y + py * pixelScale;
            int runStart = 0;
            int runColor = buffer[py * bufW];
            for (int px = 1; px <= bufW; px++) {
                int color = px < bufW ? buffer[py * bufW + px] : (runColor ^ 1);
                if (color != runColor) {
                    graphics.fill(x + runStart * pixelScale, y0, x + px * pixelScale, y0 + pixelScale, runColor);
                    runStart = px;
                    runColor = color;
                }
            }
        }
    }
}
