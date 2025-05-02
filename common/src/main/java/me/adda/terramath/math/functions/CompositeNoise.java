package me.adda.terramath.math.functions;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

public class CompositeNoise {
    private final long seed;
    private final RandomSource randomSource;

    private PerlinNoise perlinNoise;
    private SimplexNoise simplexNoise;
    private NormalNoise normalNoise;
    private BlendedNoise blendedNoise;

    public CompositeNoise(long seed) {
        this.seed = seed;
        this.randomSource = new LegacyRandomSource(seed);
    }

    /**
     * Получает значение шума Перлина в заданной точке
     */
    public double getPerlinNoise(double x, double y, double z) {
        if (perlinNoise == null) {
            perlinNoise = PerlinNoise.create(randomSource, 4, 0.25);
        }
        return perlinNoise.getValue(x, y, z);
    }

    /**
     * Получает значение шума Simplex в заданной точке (2D)
     */
    public double getSimplexNoise(double x, double z) {
        if (simplexNoise == null) {
            simplexNoise = new SimplexNoise(randomSource);
        }
        return simplexNoise.getValue(x, z);
    }

    /**
     * Получает значение нормального шума в заданной точке
     */
    public double getNormalNoise(double x, double y, double z) {
        if (normalNoise == null) {
            normalNoise = NormalNoise.create(
                    RandomSource.create(seed),
                    4,
                    0.25
            );
        }
        return normalNoise.getValue(x, y, z);
    }

    /**
     * Получает значение смешанного шума (используется для ландшафта)
     */
    public double getBlendedNoise(double x, double y, double z) {
        if (blendedNoise == null) {
            blendedNoise = new BlendedNoise(
                    randomSource,
                    0.25,  // xzScale
                    0.25,  // yScale
                    1.0,   // xzFactor
                    1.0,   // yFactor
                    1.0    // smearScaleMultiplier
            );
        }
        return blendedNoise.compute(new DensityFunction.FunctionContext() {
            @Override
            public int blockX() {
                return (int) Math.floor(x);
            }

            @Override
            public int blockY() {
                return (int) Math.floor(y);
            }

            @Override
            public int blockZ() {
                return (int) Math.floor(z);
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