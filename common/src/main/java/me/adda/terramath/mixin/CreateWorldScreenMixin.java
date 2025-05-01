package me.adda.terramath.mixin;

import me.adda.terramath.api.FormulaCacheHolder;
import me.adda.terramath.api.TerrainFormulaManager;
import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.config.ModConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin extends Screen {

    protected CreateWorldScreenMixin(Component component) {
        super(component);
    }

    @ModifyArg(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"
            ),
            index = 0
    )
    private LayoutElement terramath$modifyCancelButton(LayoutElement original) {
        if (original instanceof Button button &&
                button.getMessage().equals(CommonComponents.GUI_CANCEL)) {
            return Button.builder(CommonComponents.GUI_CANCEL, btn -> {
                terramath$resetTerrainSettings();
                ((CreateWorldScreen)(Object)this).popScreen();
            }).build();
        }
        return original;
    }

    @Unique
    private void terramath$resetTerrainSettings() {
        TerrainFormulaManager.getInstance().setFormula("");
        TerrainSettingsManager.getInstance().resetToDefaults();

        FormulaCacheHolder.resetCache();

        if (ModConfig.get().useDefaultFormula) {
            ModConfig.updateTerrainSettingsFromConfig();
        }
    }
}