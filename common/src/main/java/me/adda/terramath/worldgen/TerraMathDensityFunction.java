package me.adda.terramath.worldgen;

import com.mojang.serialization.MapCodec;
import me.adda.terramath.api.FormulaCacheHolder;
import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.math.parser.FormulaParser;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public class TerraMathDensityFunction implements DensityFunction {

    public static final KeyDispatchDataCodec<TerraMathDensityFunction> CODEC =
            KeyDispatchDataCodec.of(MapCodec.unit(TerraMathDensityFunction::new));

    private final TerrainSettingsManager settings = TerrainSettingsManager.getInstance();

    @Override
    public double compute(FunctionContext context) {
        try {
            FormulaParser.CompiledFormula formula = FormulaCacheHolder.getParsedFormula();
            if (formula == null || formula.getOriginalExpression().trim().isEmpty()) {
                return 0.0;
            }

            double scale = settings.getCoordinateScale();
            double x = context.blockX() / scale;
            double y = context.blockY() / scale;
            double z = context.blockZ() / scale;

            boolean eq = FormulaCacheHolder.isEquationMode();
            boolean dm = settings.isUseDensityMode();
            double sf = settings.getSmoothingFactor();

            double fv = sf > 0
                    ? spatialSmooth(formula, x, y, z, sf, eq)
                    : formula.evaluate(x, y, z);

            return computeDensity(fv, y, scale, eq, dm,
                    settings.getBaseHeight(), settings.getHeightVariation());
        } catch (Exception ex) {
            return 0.0;
        }
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
            return (formula.evaluate(x,      y,      z     )
                  + formula.evaluate(x + sf, y,      z     )
                  + formula.evaluate(x - sf, y,      z     )
                  + formula.evaluate(x,      y + sf, z     )
                  + formula.evaluate(x,      y - sf, z     )
                  + formula.evaluate(x,      y,      z + sf)
                  + formula.evaluate(x,      y,      z - sf)) / 7.0;
        } else {
            return (formula.evaluate(x,      y, z     )
                  + formula.evaluate(x + sf, y, z     )
                  + formula.evaluate(x - sf, y, z     )
                  + formula.evaluate(x,      y, z + sf)
                  + formula.evaluate(x,      y, z - sf)) / 5.0;
        }
    }

    private static double computeDensity(double fv, double y, double scale,
                                         boolean eq, boolean dm,
                                         double baseHeight, double heightVariation) {
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
