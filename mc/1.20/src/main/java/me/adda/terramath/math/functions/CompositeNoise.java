package me.adda.terramath.math.functions;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

public class CompositeNoise {
    private final CompositeNoise root;
    private final long seed;
    private final RandomSource randomSource;

    private volatile PerlinNoise perlinNoise;
    private volatile SimplexNoise simplexNoise;
    private volatile NormalNoise normalNoise;
    private volatile BlendedNoise blendedNoise;

    private final NoiseMemo perlinFlat;
    private final NoiseMemo simplexMemo;

    public CompositeNoise(long seed) {
        this.root = this;
        this.seed = seed;
        this.randomSource = new LegacyRandomSource(seed);
        this.perlinFlat = null;
        this.simplexMemo = null;
    }

    private CompositeNoise(CompositeNoise root) {
        this.root = root;
        this.seed = root.seed;
        this.randomSource = root.randomSource;
        this.perlinFlat = new NoiseMemo();
        this.simplexMemo = new NoiseMemo();
    }

    public CompositeNoise threadView() {
        return new CompositeNoise(root);
    }

    public boolean isViewOf(CompositeNoise other) {
        return root == other.root;
    }

    /**
     * Получает значение шума Перлина в заданной точке
     */
    public double getPerlinNoise(double x, double y, double z) {
        if (root.perlinNoise == null) {
            synchronized (root) {
                if (root.perlinNoise == null) {
                    root.perlinNoise = PerlinNoise.create(randomSource, 4, 0.25);
                }
            }
        }
        if (y != 0.0 || perlinFlat == null) {
            return root.perlinNoise.getValue(x, y, z);
        }
        double cached = perlinFlat.find(x, z);
        if (!Double.isNaN(cached)) {
            return cached;
        }
        return perlinFlat.put(x, z, root.perlinNoise.getValue(x, 0.0, z));
    }

    /**
     * Получает значение шума Simplex в заданной точке (2D)
     */
    public double getSimplexNoise(double x, double z) {
        if (root.simplexNoise == null) {
            synchronized (root) {
                if (root.simplexNoise == null) {
                    root.simplexNoise = new SimplexNoise(randomSource);
                }
            }
        }
        if (simplexMemo == null) {
            return root.simplexNoise.getValue(x, z);
        }
        double cached = simplexMemo.find(x, z);
        if (!Double.isNaN(cached)) {
            return cached;
        }
        return simplexMemo.put(x, z, root.simplexNoise.getValue(x, z));
    }

    /**
     * Получает значение нормального шума в заданной точке
     */
    public double getNormalNoise(double x, double y, double z) {
        if (root.normalNoise == null) {
            synchronized (root) {
                if (root.normalNoise == null) {
                    root.normalNoise = NormalNoise.create(
                            RandomSource.create(seed),
                            4,
                            0.25
                    );
                }
            }
        }
        return root.normalNoise.getValue(x, y, z);
    }

    /**
     * Получает значение смешанного шума (используется для ландшафта)
     */
    public double getBlendedNoise(double x, double y, double z) {
        if (root.blendedNoise == null) {
            synchronized (root) {
                if (root.blendedNoise == null) {
                    root.blendedNoise = BlendedNoise.createUnseeded(
                            0.25, // xzScale
                            0.125, // yScale
                            80.0, // xzFactor
                            160, // yFactor
                            8.0 // smearScaleMultiplier
                    ).withNewRandom(randomSource);
                }
            }
        }
        return root.blendedNoise.compute(new DensityFunction.FunctionContext() {
            @Override
            public int blockX() {
                return (int) x;
            }

            @Override
            public int blockY() {
                return (int) y;
            }

            @Override
            public int blockZ() {
                return (int) z;
            }
        });
    }

    /**
     * Возвращает масштабированный шум для создания рельефа
     * с указанным масштабом и диапазоном значений
     */
    public double getTerrain(double x, double z, double scale, double minValue, double maxValue) {
        double noise = getPerlinNoise(x / scale, 0, z / scale);

        return minValue + (noise + 1.0) / 2.0 * (maxValue - minValue);
    }

    /**
     * Создает составной шум из нескольких октав шума Перлина
     * с убывающей амплитудой для естественных ландшафтов
     */
    public double getOctavedNoise(double x, double z, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0;

        for (int i = 0; i < octaves; i++) {
            total += getPerlinNoise(x * frequency, 0, z * frequency) * amplitude;

            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }

        return total / maxValue;
    }
}