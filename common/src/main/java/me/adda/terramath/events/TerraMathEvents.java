package me.adda.terramath.events;

import dev.architectury.event.events.common.LifecycleEvent;
import me.adda.terramath.api.TerraFormulaManager;
import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.world.TerrainData;
import net.minecraft.server.level.ServerLevel;

public class TerraMathEvents {
    public static void init() {
        LifecycleEvent.SERVER_LEVEL_LOAD.register(TerraMathEvents::onLevelLoad);
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(TerraMathEvents::onLevelUnload);
    }

    private static void onLevelLoad(ServerLevel level) {
        TerrainData data = level.getDataStorage().computeIfAbsent(
                TerrainData.factory(),
                TerrainData.DATA_ID
        );

        if (data.isFirstLoad()) {
            if (hasActivePlayerSettings()) {
                data.updateFromManagers();
                data.setFirstLoad(false);
            }
        } else {
            data.applyToManagers();
        }
    }

    private static boolean hasActivePlayerSettings() {
        TerrainSettingsManager manager = TerrainSettingsManager.getInstance();
        TerraFormulaManager formulaManager = TerraFormulaManager.getInstance();

        return !formulaManager.getFormula().isEmpty() ||
                manager.getBaseHeight() != 64.0 ||
                manager.getHeightVariation() != 32.5 ||
                manager.getSmoothingFactor() != 0.55 ||
                manager.getCoordinateScale() != 1.0 ||
                manager.isUseDensityMode();
    }

    private static void onLevelUnload(ServerLevel level) {
        TerraFormulaManager.getInstance().setFormula("");
        TerrainSettingsManager manager = TerrainSettingsManager.getInstance();
        manager.resetToDefaults();
        manager.setUseDensityMode(false);
    }
}