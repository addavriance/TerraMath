package me.adda.terramath.math.functions;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class MathExtensions {
    private static final Random random = new Random();

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
        if (x <= 0) return 0;

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

    public static double mod(double a, double b) {
        return a % b;
    }

    public static double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    public static double clamp(double x, double min, double max) {
        return Math.min(Math.max(x, min), max);
    }

    public static double rand() {
        return random.nextDouble();
    }

    public static double randrange(Number min, Number max) {
        double minVal = min.doubleValue();
        double maxVal = max.doubleValue();
        double randomValue = minVal + (random.nextDouble() * (maxVal - minVal));

        return Math.round(randomValue * 1000.0) / 1000.0;
    }

    public static double randnormal(double mean, double stddev) {
        return mean + stddev * random.nextGaussian();
    }

    public static double gcd(Number a, Number b) {
        int aVal = a.intValue();
        int bVal = b.intValue();

        while (bVal != 0) {
            int temp = bVal;
            bVal = aVal % bVal;
            aVal = temp;
        }
        return Math.abs(aVal);
    }

    public static double lcm(Number a, Number b) {
        int aVal = a.intValue();
        int bVal = b.intValue();

        if (aVal == 0 || bVal == 0) {
            return 0;
        }

        return Math.abs(aVal * bVal) / gcd(a, b);
    }

    public static int[] extendedGcd(int a, int b) { // utility function
        if (b == 0) {
            return new int[]{a, 1, 0};
        }

        int[] values = extendedGcd(b, a % b);
        int gcd = values[0];
        int x1 = values[1];
        int y1 = values[2];

        int x = y1;
        int y = x1 - (a / b) * y1;

        return new int[]{gcd, x, y};
    }

    public static int modInverse(Number a, Number m) {
        int aVal = a.intValue();
        int mVal = m.intValue();

        int[] values = extendedGcd(aVal, mVal);
        if (values[0] != 1) {
            return 0;
        } else {
            int result = values[1] % mVal;
            return result < 0 ? result + mVal : result;
        }
    }

    public static double root(double number, int degree) {
        if (degree <= 0) {
            return 1;
        }
        if (number < 0 && degree % 2 == 0) {
            return 0;
        }

        return Math.pow(number, 1.0 / degree);
    }
}