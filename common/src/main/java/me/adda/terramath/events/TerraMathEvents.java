package me.adda.terramath.events;

import dev.architectury.event.events.common.LifecycleEvent;
import me.adda.terramath.api.FormulaCacheHolder;
import me.adda.terramath.api.TerraFormulaManager;
import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.world.TerrainData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

public class TerraMathEvents {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void init() {
        LifecycleEvent.SERVER_LEVEL_LOAD.register(TerraMathEvents::onLevelLoad);
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(TerraMathEvents::onLevelUnload);
    }

    private static void onLevelLoad(ServerLevel level) {
        TerrainData data = level.getDataStorage().computeIfAbsent(
                TerrainData.factory(),
                TerrainData.DATA_ID
        );

        boolean isNewWorld = false;

        try {
            Path worldPath = level.getServer().getWorldPath(LevelResource.ROOT);
            isNewWorld = !Files.exists(worldPath.resolve("level.dat_old"));
        } catch (Exception e) {
            LOGGER.error("Error checking world files: ", e);
        }

        if (isNewWorld) {
            data.updateFromManagers();
            data.setDirty();
        } else {
            data.applyToManagers();
        }
    }

    private static void onLevelUnload(ServerLevel level) {
        TerraFormulaManager.getInstance().setFormula("");
        TerrainSettingsManager manager = TerrainSettingsManager.getInstance();
        FormulaCacheHolder.resetCache();

        manager.resetToDefaults();
        manager.setUseDensityMode(false);
    }
}