package me.adda.terramath.worldgen;

import com.mojang.serialization.MapCodec;
import me.adda.terramath.api.FormulaCacheHolder;
import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.math.parser.FormulaParser;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Arrays;

public class TerraMathDensityFunction implements DensityFunction {

    public static final KeyDispatchDataCodec<TerraMathDensityFunction> CODEC =
            KeyDispatchDataCodec.of(MapCodec.unit(TerraMathDensityFunction::new));

    private final TerrainSettingsManager settings = TerrainSettingsManager.getInstance();

    private static final class ColumnCache {
        final double[] values = new double[256];
        final long[] keys = new long[256];
        FormulaParser.CompiledFormula formula;
        double scale, smoothing;

        ColumnCache() {
            Arrays.fill(keys, Long.MIN_VALUE);
        }

        boolean matches(FormulaParser.CompiledFormula formula, double scale, double smoothing) {
            if (this.formula == formula && this.scale == scale && this.smoothing == smoothing) return true;
            Arrays.fill(keys, Long.MIN_VALUE);
            this.formula = formula;
            this.scale = scale;
            this.smoothing = smoothing;
            return false;
        }
    }

    private static final ThreadLocal<ColumnCache> COLUMNS = ThreadLocal.withInitial(ColumnCache::new);

    @Override
    public double compute(FunctionContext context) {
        try {
            FormulaCacheHolder.Snapshot snapshot = FormulaCacheHolder.snapshot();
            if (snapshot == null) {
                return 0.0;
            }
            FormulaParser.CompiledFormula formula = snapshot.formula();

            double scale = settings.getCoordinateScale();
            boolean dm = settings.isUseDensityMode();
            double sf = dm ? settings.getSmoothingFactor() : 0.0;
            int blockX = context.blockX(), blockZ = context.blockZ();
            double x = blockX / scale;
            double y = context.blockY() / scale;
            double z = blockZ / scale;

            boolean eq = snapshot.equation();

            double fv;
            if (snapshot.usesY()) {
                fv = formulaValue(formula, x, y, z, sf, eq);
            } else {
                ColumnCache cache = COLUMNS.get();
                cache.matches(formula, scale, sf);
                int slot = ((blockX & 15) << 4) | (blockZ & 15);
                long key = ((long) blockX << 32) ^ (blockZ & 0xffffffffL);
                if (cache.keys[slot] == key) {
                    fv = cache.values[slot];
                } else {
                    fv = formulaValue(formula, x, y, z, sf, eq);
                    cache.keys[slot] = key;
                    cache.values[slot] = fv;
                }
            }

            return computeDensity(fv, y, scale, eq, dm,
                    settings.getBaseHeight(), settings.getHeightVariation());
        } catch (Exception ex) {
            return 0.0;
        }
    }

    private static double formulaValue(FormulaParser.CompiledFormula formula,
                                       double x, double y, double z, double sf, boolean eq) {
        return sf > 0
                ? spatialSmooth(formula, x, y, z, sf, eq)
                : formula.evaluate(x, y, z);
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        for (int i = 0; i < array.length; i++) {
            array[i] = this.compute(contextProvider.forIndex(i));
        }
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(this);
    }

    public DensityFunction mapChildren(Visitor visitor) {
        return this;
    }

    @Override
    public double minValue() {
        return -1.0;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }

    private static double spatialSmooth(FormulaParser.CompiledFormula formula,
                                        double x, double y, double z,
                                        double sf, boolean eq) {
        if (eq) {
            return (formula.evaluate(x, y, z)
                  + formula.evaluate(x + sf, y, z)
                  + formula.evaluate(x - sf, y, z)
                  + formula.evaluate(x, y + sf, z)
                  + formula.evaluate(x, y - sf, z)
                  + formula.evaluate(x, y, z + sf)
                  + formula.evaluate(x, y, z - sf)) / 7.0;
        } else {
            return (formula.evaluate(x, y, z)
                  + formula.evaluate(x + sf, y, z)
                  + formula.evaluate(x - sf, y, z)
                  + formula.evaluate(x, y, z + sf)
                  + formula.evaluate(x, y, z - sf)) / 5.0;
        }
    }

    private static double computeDensity(double fv, double y, double scale,
                                         boolean eq, boolean dm,
                                         double baseHeight, double heightVariation) {
        if (!Double.isFinite(fv)) {
            return -1.0;
        }
        final double d;
        if (eq) {
            d = dm ? (fv - (baseHeight - 64.0) / scale) / Math.max(1e-6, heightVariation) : fv;
        } else {
            double tgt = dm ? baseHeight / scale + fv * heightVariation : 64.0 / scale + fv;
            d = tgt - y;
        }
        return d >= 0 ? 1.0 : -1.0;
    }
}
