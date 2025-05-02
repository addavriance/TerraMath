package me.adda.terramath.mixin;

import me.adda.terramath.config.ConfigScreenFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$WorldTab")
public abstract class WorldTabMixin extends GridLayoutTab {
    private final Minecraft minecraft = Minecraft.getInstance();
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private Button terrainSettingsButton;

    public WorldTabMixin(Component component) {
        super(component);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        if (this.layout != null) {
            GridLayout.RowHelper rowHelper = this.layout.columnSpacing(10).rowSpacing(8).createRowHelper(2);

            // Skip a few rows to align with original interface
            for (int i = 0; i < 5; i++) {
                rowHelper.addChild(new GridLayout());
            }

            GridLayout settingsLayout = new GridLayout();
            settingsLayout.rowSpacing(4);
            GridLayout.RowHelper settingsRowHelper = settingsLayout.createRowHelper(1);

            // Add terrain settings button - now points to ConfigScreen
            this.terrainSettingsButton = Button.builder(
                    Component.translatable("terramath.config.terrain_settings"),
                    button -> {
                        if (minecraft.screen instanceof CreateWorldScreen createWorldScreen) {
                            minecraft.setScreen(ConfigScreenFactory.createScreen(createWorldScreen, true));
                        }
                    }
            ).size(BUTTON_WIDTH, BUTTON_HEIGHT).build();

            settingsRowHelper.addChild(this.terrainSettingsButton);

            // Add section header and tooltip
            StringWidget label = new StringWidget(
                    Component.translatable("terramath.config.terrain_settings_label"),
                    minecraft.font
            ).alignLeft();
            settingsRowHelper.addChild(label);

            // Add layout to original position in interface
            rowHelper.addChild(settingsLayout, 2);

            // Set world type change handler
            if (minecraft.screen instanceof CreateWorldScreen createWorldScreen) {
                createWorldScreen.getUiState().addListener((worldCreationUiState -> {
                    List<WorldCreationUiState.WorldTypeEntry> worldTypeList = worldCreationUiState.getNormalPresetList();
                    boolean isFirstWorldType = worldTypeList.indexOf(worldCreationUiState.getWorldType()) == 0;

                    // Only enable button for first (basic) world type
                    this.terrainSettingsButton.active = isFirstWorldType;
                }));
            }
        }
    }
}