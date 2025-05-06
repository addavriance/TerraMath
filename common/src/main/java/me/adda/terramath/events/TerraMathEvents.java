package me.adda.terramath.events;

import me.adda.terramath.api.FormulaCacheHolder;
import me.adda.terramath.api.TerrainFormulaManager;
import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.config.ModConfig;
import me.adda.terramath.world.TerrainData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class TerraMathEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger("TerraMath/TerraMathEvents");

    public static void onLevelLoad(ServerLevel level) {
        TerrainData data = level.getDataStorage().computeIfAbsent(
                TerrainData::load,
                TerrainData::create,
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

            FormulaCacheHolder.resetCache();
        }
    }

    public static void onLevelUnload(ServerLevel level) {
        TerrainFormulaManager.getInstance().setFormula("");
        TerrainSettingsManager manager = TerrainSettingsManager.getInstance();
        FormulaCacheHolder.resetCache();

        manager.resetToDefaults();
        manager.setUseDensityMode(false);

        if (ModConfig.get().useDefaultFormula) {
            ModConfig.updateTerrainSettingsFromConfig();
        }
    }
}