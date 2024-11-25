package me.adda.terramath.events;

import dev.architectury.event.events.common.LifecycleEvent;
import me.adda.terramath.api.TerraFormulaManager;
import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.world.TerrainData;
import net.minecraft.server.level.ServerLevel;

public class TerraMathEvents {
    public static void init() {
        LifecycleEvent.SERVER_LEVEL_LOAD.register(TerraMathEvents::onLevelLoad);
        LifecycleEvent.SERVER_LEVEL_SAVE.register(TerraMathEvents::onLevelSave);
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(TerraMathEvents::onLevelUnload);
    }

    private static void onLevelLoad(ServerLevel level) {
        TerrainData data = level.getDataStorage().get(TerrainData::load, TerrainData.IDENTIFIER.toString());

        if (data != null) {
            data.applyToManagers();
        }
    }

    private static void onLevelSave(ServerLevel level) {
        TerrainData data = new TerrainData();
        data.updateFromManagers();

        if (!data.formula.trim().isEmpty()) {
            level.getDataStorage().set(TerrainData.IDENTIFIER.toString(), data);
        }
    }

    private static void onLevelUnload(ServerLevel level) {
        TerraFormulaManager.getInstance().setFormula("");
        TerrainSettingsManager manager = TerrainSettingsManager.getInstance();
        manager.setBaseHeight(64.0);
        manager.setHeightVariation(32.5);
        manager.setSmoothingFactor(0.55);
        manager.setUseDensityMode(false);
    }
}