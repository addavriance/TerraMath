package me.adda.terramath.mixin;

import me.adda.terramath.api.FormulaCacheHolder;
import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.math.ParsedFormula;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoiseRouter.class)
public class NoiseRouterMixin {

    @Inject(method = "finalDensity", at = @At("HEAD"), cancellable = true)
    private void injectCustomTerrainNoise(CallbackInfoReturnable<DensityFunction> cir) {
        ParsedFormula formula = FormulaCacheHolder.getParsedFormula();
        if (formula == null || formula.getOriginalExpression().trim().isEmpty()) {
            return;
        }

        DensityFunction customTerrainFunction = new DensityFunction() {
            private final TerrainSettingsManager settings = TerrainSettingsManager.getInstance();

            @Override
            public double compute(FunctionContext context) {
                try {
                    double x = context.blockX() / 10.0;
                    double z = context.blockZ() / 10.0;

                    double formulaValue = formula.evaluate(x, z);

                    if (!settings.isUseDensityMode()) {
                        double y = context.blockY();
                        double targetHeight = 64 + (formulaValue * 32);
                        return y < targetHeight ? 1.0 : -1.0;
                    } else {
                        double targetHeight = settings.getBaseHeight() + (formulaValue * settings.getHeightVariation());
                        double y = context.blockY();
                        double distanceFromTarget = Math.abs(y - targetHeight);

                        if (y < targetHeight) {
                            return 1.0 - (distanceFromTarget / targetHeight) * settings.getSmoothingFactor();
                        } else {
                            return -((distanceFromTarget / settings.getHeightVariation()) * settings.getSmoothingFactor());
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Error computing terrain density: " + ex.getMessage());
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
                return null;
            }
        };

        cir.setReturnValue(customTerrainFunction);
    }
}