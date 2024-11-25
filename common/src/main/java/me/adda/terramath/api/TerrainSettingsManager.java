package me.adda.terramath.api;

import me.adda.terramath.gui.TerrainSettingsSlider.TerrainSettingType;

public class TerrainSettingsManager {
    private static final TerrainSettingsManager INSTANCE = new TerrainSettingsManager();

    private double baseHeight = 64.0;
    private double heightVariation = 32.5;
    private double smoothingFactor = 0.55;
    private boolean useDensityMode = false;

    private TerrainSettingsManager() {}

    public static TerrainSettingsManager getInstance() {
        return INSTANCE;
    }

    public void setTerrainSetting(TerrainSettingType type, double value) {
        switch (type) {
            case BASE_HEIGHT -> setBaseHeight(value);
            case HEIGHT_VARIATION -> setHeightVariation(value);
            case SMOOTHING_FACTOR -> setSmoothingFactor(value);
        }
    }

    public double getBaseHeight() {
        return baseHeight;
    }

    public void setBaseHeight(double baseHeight) {
        this.baseHeight = baseHeight;
    }

    public double getHeightVariation() {
        return heightVariation;
    }

    public void setHeightVariation(double heightVariation) {
        this.heightVariation = heightVariation;
    }

    public double getSmoothingFactor() {
        return smoothingFactor;
    }

    public void setSmoothingFactor(double smoothingFactor) {
        this.smoothingFactor = smoothingFactor;
    }

    public boolean isUseDensityMode() {
        return useDensityMode;
    }

    public void setUseDensityMode(boolean useDensityMode) {
        this.useDensityMode = useDensityMode;
    }
}