package me.adda.terramath.mixin;

import me.adda.terramath.api.FormulaCacheHolder;
import me.adda.terramath.worldgen.TerraMathDensityFunction;
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
        if (FormulaCacheHolder.getParsedFormula() == null
                || FormulaCacheHolder.getParsedFormula().getOriginalExpression().trim().isEmpty()) {
            return;
        }
        cir.setReturnValue(new TerraMathDensityFunction());
    }
}
