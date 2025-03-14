package me.adda.terramath.mixin;

import me.adda.terramath.config.ModConfig;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DimensionType.class)
public class WorldHeightMixin {

    @Inject(method = "logicalHeight", at = @At("HEAD"), cancellable = true)
    private void onGetLogicalHeight(CallbackInfoReturnable<Integer> cir) {
        if (ModConfig.get().customWorldHeight) {
            cir.setReturnValue(ModConfig.get().worldHeight);
        }
    }

    @Inject(method = "minY", at = @At("HEAD"), cancellable = true)
    private void onGetMinY(CallbackInfoReturnable<Integer> cir) {
        if (ModConfig.get().customWorldHeight) {
            ModConfig config = ModConfig.get();

            if (config.symmetricHeight) {
                cir.setReturnValue(-config.worldHeight / 2);
            } else {
                cir.setReturnValue(-64);
            }
        }
    }

    @Inject(method = "height", at = @At("HEAD"), cancellable = true)
    private void onGetHeight(CallbackInfoReturnable<Integer> cir) {
        if (ModConfig.get().customWorldHeight) {
            ModConfig config = ModConfig.get();

            if (config.symmetricHeight) {
                cir.setReturnValue(config.worldHeight);
            } else {
                cir.setReturnValue(64 + config.worldHeight);
            }
        }
    }
}