package me.adda.terramath.math.functions;

public final class NoiseMemo {
    private static final int SIZE = 1024;
    private static final int MASK = SIZE - 1;

    private final double[] xs = new double[SIZE];
    private final double[] zs = new double[SIZE];

    private final double[] values = new double[SIZE];
    private final boolean[] filled = new boolean[SIZE];

    private static int slot(double x, double z) {
        long bits = Double.doubleToLongBits(x) * 31 + Double.doubleToLongBits(z);

        return (int) (bits ^ (bits >>> 29) ^ (bits >>> 47)) & MASK;
    }

    public double find(double x, double z) {
        int slot = slot(x, z);

        if (filled[slot] && xs[slot] == x && zs[slot] == z) {
            return values[slot];
        }

        return Double.NaN;
    }

    public double put(double x, double z, double value) {
        int slot = slot(x, z);

        xs[slot] = x;
        zs[slot] = z;

        values[slot] = value;
        filled[slot] = true;

        return value;
    }
}
