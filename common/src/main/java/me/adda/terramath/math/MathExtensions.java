package me.adda.terramath.math;

import org.joml.SimplexNoise;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MathExtensions {
    private static final double[] LANCZOS_COEFFICIENTS = {
            676.5203681218851,
            -1259.1392167224028,
            771.32342877765313,
            -176.61502916214059,
            12.507343278686905,
            -0.13857109526572012,
            9.9843695780195716e-6,
            1.5056327351493116e-7
    };

    private static final Map<Integer, Double> GAMMA_CACHE = new ConcurrentHashMap<>();

    public static double gamma(double x) {
        if (x > 0 && x == Math.floor(x) && x <= 170) {
            int n = (int) x;
            return GAMMA_CACHE.computeIfAbsent(n, MathExtensions::calculateGamma);
        }

        return calculateGamma(x);
    }

    private static double calculateGamma(double x) {
        if (x < 0.5) {
            return Math.PI / (Math.sin(Math.PI * x) * gamma(1 - x));
        }

        x -= 1;
        double t = 0.99999999999980993;
        for (int i = 0; i < LANCZOS_COEFFICIENTS.length; i++) {
            t += LANCZOS_COEFFICIENTS[i] / (x + i + 1);
        }

        return Math.sqrt(2 * Math.PI) * Math.pow(x + 7.5, x + 0.5) *
                Math.exp(-(x + 7.5)) * t;
    }

    public static double beta(double x, double y) {
        return Math.exp(logGamma(x) + logGamma(y) - logGamma(x + y));
    }

    private static double logGamma(double x) {
        if (x <= 0) return Double.NaN;

        double tmp = (x - 0.5) * Math.log(x + 4.5) - (x + 4.5);
        double ser = 1.0 + 76.18009173 / (x + 0) - 86.50532033 / (x + 1)
                + 24.01409822 / (x + 2) - 1.231739516 / (x + 3)
                + 0.00120858003 / (x + 4) - 0.00000536382 / (x + 5);
        return tmp + Math.log(ser * Math.sqrt(2 * Math.PI));
    }

    public static double erf(double x) {
        double t = 1.0 / (1.0 + 0.5 * Math.abs(x));


        double[] c = {
                -1.26551223, 1.00002368, 0.37409196,
                0.09678418, -0.18628806, 0.27886807,
                -1.13520398, 1.48851587, -0.82215223,
                0.17087277
        };

        double tau = t * Math.exp(-x * x + c[0] + t * (c[1] + t * (c[2] + t * (c[3] + t *
                (c[4] + t * (c[5] + t * (c[6] + t * (c[7] + t * (c[8] + t * c[9])))))))));

        return x >= 0 ? 1.0 - tau : tau - 1.0;
    }

    public static double noise(double x, double y, double z) {
        return SimplexNoise.noise((float) x, (float) y, (float) z);
    }
}