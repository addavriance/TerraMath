package me.adda.terramath.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.adda.terramath.api.FormulaCacheHolder;
import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.api.TerrainSettingsManager.TerrainSettingType;
import me.adda.terramath.api.TerrainFormulaManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig INSTANCE;
    private static File configFile;

    // Formula settings
    public String baseFormula = "";
    public boolean useDefaultFormula = true;

    // Terrain parameters
    public double coordinateScale = TerrainSettingsManager.getDefaultByType(TerrainSettingType.COORDINATE_SCALE);
    public double baseHeight = TerrainSettingsManager.getDefaultByType(TerrainSettingType.BASE_HEIGHT);
    public double heightVariation = TerrainSettingsManager.getDefaultByType(TerrainSettingType.HEIGHT_VARIATION);
    public double smoothingFactor = TerrainSettingsManager.getDefaultByType(TerrainSettingType.SMOOTHING_FACTOR);
    public boolean useDensityMode = false;

    public NoiseType noiseType = NoiseType.NONE;
    public double noiseScaleX = TerrainSettingsManager.getDefaultByType(TerrainSettingType.NOISE_SCALE_X);
    public double noiseScaleY = TerrainSettingsManager.getDefaultByType(TerrainSettingType.NOISE_SCALE_Y);
    public double noiseScaleZ = TerrainSettingsManager.getDefaultByType(TerrainSettingType.NOISE_SCALE_Z);
    public double noiseHeightScale = TerrainSettingsManager.getDefaultByType(TerrainSettingType.NOISE_HEIGHT_SCALE);

    private static Consumer<ModConfig> saveCallback = null;

    public static void init(Path configDir) {
        configFile = configDir.resolve("terramath.json").toFile();

        if (INSTANCE == null) {
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    INSTANCE = GSON.fromJson(reader, ModConfig.class);
                } catch (IOException e) {
                    INSTANCE = new ModConfig();
                    save();
                }
            } else {
                INSTANCE = new ModConfig();
                save();
            }

            initializeDefaultValues();
        }
    }

    private static void initializeDefaultValues() {
        if (INSTANCE.baseFormula == null || INSTANCE.baseFormula.isEmpty()) {
            INSTANCE.baseFormula = "";
        }

        if (INSTANCE.useDefaultFormula) {
            updateTerrainSettingsFromConfig();
            FormulaCacheHolder.resetCache();
        }
    }

    /**
     * Updates managers from config values
     */
    public static void updateTerrainSettingsFromConfig() {
        if (INSTANCE != null) {
            TerrainFormulaManager.getInstance().setFormula(INSTANCE.baseFormula);

            TerrainSettingsManager settings = TerrainSettingsManager.getInstance();

            settings.setCoordinateScale(INSTANCE.coordinateScale);
            settings.setBaseHeight(INSTANCE.baseHeight);
            settings.setHeightVariation(INSTANCE.heightVariation);
            settings.setSmoothingFactor(INSTANCE.smoothingFactor);
            settings.setUseDensityMode(INSTANCE.useDensityMode);

            settings.setNoiseType(INSTANCE.noiseType);
            settings.setNoiseScaleX(INSTANCE.noiseScaleX);
            settings.setNoiseScaleY(INSTANCE.noiseScaleY);
            settings.setNoiseScaleZ(INSTANCE.noiseScaleZ);
            settings.setNoiseHeightScale(INSTANCE.noiseHeightScale);
        }
    }

    public static void updateConfigFromTerrainSettings() {
        if (INSTANCE != null) {
            INSTANCE.baseFormula = TerrainFormulaManager.getInstance().getFormula();

            TerrainSettingsManager settings = TerrainSettingsManager.getInstance();

            INSTANCE.coordinateScale = settings.getCoordinateScale();
            INSTANCE.baseHeight = settings.getBaseHeight();
            INSTANCE.heightVariation = settings.getHeightVariation();
            INSTANCE.smoothingFactor = settings.getSmoothingFactor();
            INSTANCE.useDensityMode = settings.isUseDensityMode();

            INSTANCE.noiseType = settings.getNoiseType();
            INSTANCE.noiseScaleX = settings.getNoiseScaleX();
            INSTANCE.noiseScaleY = settings.getNoiseScaleY();
            INSTANCE.noiseScaleZ = settings.getNoiseScaleZ();
            INSTANCE.noiseHeightScale = settings.getNoiseHeightScale();
        }
    }

    public static ModConfig get() {
        if (INSTANCE == null) {
            INSTANCE = new ModConfig();
            initializeDefaultValues();
            save();
        }
        return INSTANCE;
    }

    public static void save() {
        try {
            if (!configFile.getParentFile().exists()) {
                Files.createDirectories(configFile.getParentFile().toPath());
            }

            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(INSTANCE, writer);
            }

            if (saveCallback != null) {
                saveCallback.accept(INSTANCE);
            }
        } catch (IOException e) {
            System.err.println("Failed to save TerraMath config: " + e.getMessage());
        }
    }

    public static void setSaveCallback(Consumer<ModConfig> callback) {
        saveCallback = callback;
    }
}