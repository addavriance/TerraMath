package me.adda.terramath.math.formula;

import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.math.parser.FormulaParser;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;
import java.util.Random;

final class FormulaFit {
    private static final double HALF = 100;
    private static final double Y_MIN = -64;
    private static final double Y_MAX = 320;
    private static final int GRID = 32;
    private static final int EQ_XZ = 20;
    private static final int EQ_Y = 36;

    private FormulaFit() {}

    static String num(double v) {
        String s = new BigDecimal(v).round(new MathContext(3)).stripTrailingZeros().toPlainString();
        return s.equals("-0") ? "0" : s;
    }

    private static String affine(double a, double b, String expr) {
        String body = num(a) + " * " + expr;
        if (Math.abs(b) < 1e-3) return body;
        return body + (b < 0 ? " - " : " + ") + num(Math.abs(b));
    }

    record Profile(double lowMin, double lowMax, double highMin, double highMax,
                   double quantile, double plateau) {
        static Profile of(double lowMin, double lowMax, double highMin, double highMax) {
            return new Profile(lowMin, lowMax, highMin, highMax, 0.05, 0.45);
        }

        Profile spread(double quantile, double plateau) {
            return new Profile(lowMin, lowMax, highMin, highMax, quantile, plateau);
        }
    }

    static String heightmap(String raw, Profile profile, Random rng, TerrainSettingsManager s) {
        FormulaParser.CompiledFormula compiled = FormulaParser.tryParse(raw);
        if (compiled == null) return null;

        double cs = s.getCoordinateScale();
        double surfaceBase = s.isUseDensityMode() ? s.getBaseHeight() : 64.0;
        double k = s.isUseDensityMode() ? cs * s.getHeightVariation() : cs;
        if (k < 1e-6) return null;

        double[] r = new double[GRID * GRID];
        for (int i = 0; i < GRID; i++) {
            for (int j = 0; j < GRID; j++) {
                double v = sample(compiled, cell(i, GRID) / cs, 0, cell(j, GRID) / cs);
                if (!Double.isFinite(v)) return null;
                r[i * GRID + j] = v;
            }
        }

        double[] sorted = r.clone();
        Arrays.sort(sorted);
        double lo = sorted[(int) Math.min(sorted.length - 1, sorted.length * profile.quantile())];
        double hi = sorted[(int) Math.min(sorted.length - 1, sorted.length * (1 - profile.quantile()))];
        if (hi - lo < 1e-6) return null;

        double low = profile.lowMin() + rng.nextDouble() * (profile.lowMax() - profile.lowMin());
        double high = profile.highMin() + rng.nextDouble() * (profile.highMax() - profile.highMin());
        double[] h = new double[r.length];
        for (int i = 0; i < r.length; i++) {
            h[i] = Math.max(Y_MIN, Math.min(Y_MAX, low + (high - low) * (r[i] - lo) / (hi - lo)));
        }
        if (!goodHeights(h, profile.plateau())) return null;

        double a = (high - low) / (k * (hi - lo));
        double b = (low - surfaceBase) / k - a * lo;
        return affine(a, b, FormulaGenerator.wrap(raw));
    }

    static boolean checkHeights(String formula, double plateau, TerrainSettingsManager s) {
        FormulaParser.CompiledFormula compiled = FormulaParser.tryParse(formula);
        if (compiled == null) return false;

        double cs = s.getCoordinateScale();
        double surfaceBase = s.isUseDensityMode() ? s.getBaseHeight() : 64.0;
        double k = s.isUseDensityMode() ? cs * s.getHeightVariation() : cs;
        double[] h = new double[GRID * GRID];
        for (int i = 0; i < GRID; i++) {
            for (int j = 0; j < GRID; j++) {
                double v = sample(compiled, cell(i, GRID) / cs, 0, cell(j, GRID) / cs);
                if (!Double.isFinite(v)) return false;
                h[i * GRID + j] = Math.max(Y_MIN, Math.min(Y_MAX, surfaceBase + k * v));
            }
        }
        return goodHeights(h, plateau);
    }

    static boolean checkVolume(String formula, TerrainSettingsManager s) {
        FormulaParser.CompiledFormula compiled = FormulaParser.tryParse(formula);
        if (compiled == null) return false;

        double cs = s.getCoordinateScale();
        double shift = s.isUseDensityMode() ? (s.getBaseHeight() - 64.0) / cs : 0;
        double[] v = new double[EQ_XZ * EQ_Y * EQ_XZ];
        for (int i = 0; i < EQ_XZ; i++) {
            for (int j = 0; j < EQ_Y; j++) {
                for (int l = 0; l < EQ_XZ; l++) {
                    double value = sample(compiled, cell(i, EQ_XZ) / cs,
                            (Y_MIN + (j + 0.5) * (Y_MAX - Y_MIN) / EQ_Y) / cs, cell(l, EQ_XZ) / cs);
                    if (!Double.isFinite(value)) return false;
                    v[(i * EQ_Y + j) * EQ_XZ + l] = shift - value;
                }
            }
        }
        return goodVolume(v, 0, 0.05);
    }

    private static boolean goodHeights(double[] h, double plateau) {
        double mean = Arrays.stream(h).average().orElse(0);
        double fill = (mean - Y_MIN) / (Y_MAX - Y_MIN);
        double var = Arrays.stream(h).map(v -> (v - mean) * (v - mean)).average().orElse(0);
        if (fill < 0.12 || fill > 0.6 || Math.sqrt(var) < 8) return false;

        int[] bins = new int[16];
        double min = Arrays.stream(h).min().orElse(0);
        double max = Arrays.stream(h).max().orElse(1);
        if (max - min < 25) return false;
        for (double v : h) bins[Math.min(15, (int) ((v - min) / (max - min) * 16))]++;
        if (Arrays.stream(bins).max().orElse(0) > h.length * plateau) return false;

        double sx = 0, sz = 0, sxx = 0, szz = 0, sxh = 0, szh = 0;
        for (int i = 0; i < GRID; i++) {
            for (int j = 0; j < GRID; j++) {
                double dh = h[i * GRID + j] - mean;
                double dx = i - (GRID - 1) / 2.0, dz = j - (GRID - 1) / 2.0;
                sxx += dx * dx; szz += dz * dz; sxh += dx * dh; szh += dz * dh;
            }
        }
        double explained = (sxh * sxh / sxx + szh * szh / szz) / (var * h.length);

        double products = 0;
        for (int i = 0; i < GRID; i++) {
            for (int j = 1; j < GRID; j++) {
                products += (h[i * GRID + j] - mean) * (h[i * GRID + j - 1] - mean)
                        + (h[j * GRID + i] - mean) * (h[(j - 1) * GRID + i] - mean);
            }
        }
        double correlation = products / (2.0 * GRID * (GRID - 1)) / var;
        return explained < 0.7 && correlation > 0.55;
    }

    static String equation(String lhs, double fillMin, double fillMax, Random rng, TerrainSettingsManager s) {
        FormulaParser.CompiledFormula compiled = FormulaParser.tryParse(lhs);
        if (compiled == null) return null;

        double cs = s.getCoordinateScale();
        double[] v = new double[EQ_XZ * EQ_Y * EQ_XZ];
        for (int i = 0; i < EQ_XZ; i++) {
            for (int j = 0; j < EQ_Y; j++) {
                for (int l = 0; l < EQ_XZ; l++) {
                    double value = sample(compiled, cell(i, EQ_XZ) / cs,
                            (Y_MIN + (j + 0.5) * (Y_MAX - Y_MIN) / EQ_Y) / cs, cell(l, EQ_XZ) / cs);
                    if (!Double.isFinite(value)) return null;
                    v[(i * EQ_Y + j) * EQ_XZ + l] = value;
                }
            }
        }

        double[] sorted = v.clone();
        Arrays.sort(sorted);
        double fill = fillMin + rng.nextDouble() * (fillMax - fillMin);
        double c = sorted[(int) (sorted.length * fill)];
        if (!goodVolume(v, c, fillMin * 0.5)) return null;

        double shift = s.isUseDensityMode() ? (s.getBaseHeight() - 64.0) / cs : 0;
        return lhs + " = " + num(c + shift);
    }

    private static boolean goodVolume(double[] v, double c, double minFill) {
        int solid = 0, flipsXZ = 0, flipsY = 0, pairsXZ = 0, pairsY = 0;
        double[] top = new double[EQ_XZ * EQ_XZ];
        Arrays.fill(top, Y_MIN);
        for (int i = 0; i < EQ_XZ; i++) {
            for (int j = 0; j < EQ_Y; j++) {
                for (int l = 0; l < EQ_XZ; l++) {
                    boolean in = v[(i * EQ_Y + j) * EQ_XZ + l] <= c;
                    if (in) {
                        solid++;
                        top[i * EQ_XZ + l] = Y_MIN + (j + 0.5) * (Y_MAX - Y_MIN) / EQ_Y;
                    }
                    if (i > 0) { pairsXZ++; if (in != (v[((i - 1) * EQ_Y + j) * EQ_XZ + l] <= c)) flipsXZ++; }
                    if (l > 0) { pairsXZ++; if (in != (v[(i * EQ_Y + j) * EQ_XZ + l - 1] <= c)) flipsXZ++; }
                    if (j > 0) { pairsY++; if (in != (v[(i * EQ_Y + j - 1) * EQ_XZ + l] <= c)) flipsY++; }
                }
            }
        }
        double fill = solid / (double) v.length;
        double edges = (flipsXZ + flipsY) / (double) (pairsXZ + pairsY);
        double mean = Arrays.stream(top).average().orElse(0);
        double std = Math.sqrt(Arrays.stream(top).map(t -> (t - mean) * (t - mean)).average().orElse(0));
        return fill > minFill && fill < 0.6 && edges > 0.02 && edges < 0.25
                && flipsXZ / (double) pairsXZ > 0.02 && std > 8;
    }

    private static double cell(int i, int n) {
        return -HALF + (i + 0.5) * 2 * HALF / n;
    }

    private static double sample(FormulaParser.CompiledFormula f, double x, double y, double z) {
        try {
            return f.evaluate(x, y, z);
        } catch (RuntimeException e) {
            return Double.NaN;
        }
    }
}
