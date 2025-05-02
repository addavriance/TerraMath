package me.adda.terramath.api;

import me.adda.terramath.config.NoiseType;

public class TerrainSettingsManager {
    private static final TerrainSettingsManager INSTANCE = new TerrainSettingsManager();

    private double baseHeight = getDefaultByType(TerrainSettingType.BASE_HEIGHT);
    private double heightVariation = getDefaultByType(TerrainSettingType.HEIGHT_VARIATION);
    private double smoothingFactor = getDefaultByType(TerrainSettingType.SMOOTHING_FACTOR);
    private double coordinateScale = getDefaultByType(TerrainSettingType.COORDINATE_SCALE);
    private boolean useDensityMode = false;

    private NoiseType noiseType = NoiseType.NONE;
    private double noiseScaleX = getDefaultByType(TerrainSettingType.NOISE_SCALE_X);
    private double noiseScaleY = getDefaultByType(TerrainSettingType.NOISE_SCALE_Y);
    private double noiseScaleZ = getDefaultByType(TerrainSettingType.NOISE_SCALE_Z);
    private double noiseHeightScale = getDefaultByType(TerrainSettingType.NOISE_HEIGHT_SCALE);

    public enum TerrainSettingType {
        COORDINATE_SCALE,
        BASE_HEIGHT,
        HEIGHT_VARIATION,
        SMOOTHING_FACTOR,
        NOISE_SCALE_X,
        NOISE_SCALE_Y,
        NOISE_SCALE_Z,
        NOISE_HEIGHT_SCALE
    }

    public static TerrainSettingsManager getInstance() {
        return INSTANCE;
    }

    public void setTerrainSetting(TerrainSettingType type, double value) {
        switch (type) {
            case COORDINATE_SCALE -> setCoordinateScale(value);
            case BASE_HEIGHT -> setBaseHeight(value);
            case HEIGHT_VARIATION -> setHeightVariation(value);
            case SMOOTHING_FACTOR -> setSmoothingFactor(value);
            case NOISE_SCALE_X -> setNoiseScaleX(value);
            case NOISE_SCALE_Y -> setNoiseScaleY(value);
            case NOISE_SCALE_Z -> setNoiseScaleZ(value);
            case NOISE_HEIGHT_SCALE -> setNoiseHeightScale(value);
        }
    }

    public double getCoordinateScale() {
        return coordinateScale;
    }

    public void setCoordinateScale(double coordinateScale) {
        this.coordinateScale = coordinateScale;
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

    public void setNoiseType(NoiseType noiseType) {
        this.noiseType = noiseType;
    }

    public void setNoiseScaleX(double scaleX) {
        this.noiseScaleX = scaleX;
    }

    public void setNoiseScaleY(double scaleY) {
        this.noiseScaleY = scaleY;
    }

    public void setNoiseScaleZ(double scaleZ) {
        this.noiseScaleZ = scaleZ;
    }

    public void setNoiseHeightScale(double noiseHeightScale) {
        this.noiseHeightScale = noiseHeightScale;
    }

    public NoiseType getNoiseType() {
        return noiseType;
    }

    public double getNoiseScaleX() {
        return noiseScaleX;
    }

    public double getNoiseScaleY() {
        return noiseScaleY;
    }

    public double getNoiseScaleZ() {
        return noiseScaleZ;
    }

    public double getNoiseHeightScale() {
        return noiseHeightScale;
    }

    public static double getDefaultByType(TerrainSettingType type) {
        return switch (type) {
            case COORDINATE_SCALE -> 5.0;
            case BASE_HEIGHT -> 64.0;
            case HEIGHT_VARIATION -> 32.5;
            case SMOOTHING_FACTOR -> 0.55;
            case NOISE_SCALE_X, NOISE_SCALE_Y, NOISE_SCALE_Z -> 30.0;
            case NOISE_HEIGHT_SCALE -> 4.0;
        };
    }

    public void resetToDefaults() {
        for (TerrainSettingType type : TerrainSettingType.values()) {
            setTerrainSetting(type, getDefaultByType(type));
        }

        this.useDensityMode = false;
    }
}