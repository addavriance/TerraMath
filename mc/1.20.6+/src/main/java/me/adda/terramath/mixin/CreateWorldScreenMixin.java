package me.adda.terramath.mixin;

import me.adda.terramath.api.FormulaCacheHolder;
import me.adda.terramath.api.TerrainFormulaManager;
import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.config.ModConfig;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin extends Screen {

    protected CreateWorldScreenMixin(Component component) {
        super(component);
    }

    @Redirect(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"
            )
    )
    private LayoutElement terramath$redirectAddCancelButton(LinearLayout instance, LayoutElement layoutElement) {
        if (layoutElement instanceof Button button &&
                button.getMessage().equals(CommonComponents.GUI_CANCEL)) {

            Button customCancelButton = Button.builder(
                    CommonComponents.GUI_CANCEL,
                    btn -> {
                        terramath$resetTerrainSettings();

                        ((CreateWorldScreen)(Object)this).popScreen();
                    }
            ).build();

            return instance.addChild(customCancelButton);
        }

        return instance.addChild(layoutElement);
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