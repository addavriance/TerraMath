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
        TerrainData data = level.getDataStorage().get(TerrainData::load, TerrainData.IDENTIFIER.toString());

        if (data != null) {
            data.applyToManagers();
        } else {
            TerrainData new_data = new TerrainData();
            new_data.updateFromManagers();

            level.getDataStorage().set(TerrainData.IDENTIFIER.toString(), new_data);
        }
    }

    private static void onLevelUnload(ServerLevel level) {
        TerraFormulaManager.getInstance().setFormula("");
        TerrainSettingsManager manager = TerrainSettingsManager.getInstance();

        manager.resetToDefaults();
        manager.setUseDensityMode(false);
    }
}