package me.adda.terramath.gui;

import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.api.TerrainSettingsManager.TerrainSettingType;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class TerrainSettingsSlider extends AbstractSliderButton {
    private Runnable onValueChangeListener;
    private final double minValue;
    private final double maxValue;
    private final String translationKey;
    private final TerrainSettingsManager settings;
    private final TerrainSettingsManager.TerrainSettingType type;

    public TerrainSettingsSlider(int x, int y, int width, TerrainSettingType type, TerrainSettingsManager settings) {
        super(x, y, width, 20, Component.empty(), 0.0);

        this.settings = settings;
        this.type = type;

        switch (type) {
            case BASE_HEIGHT -> {
                this.minValue = 32.0;
                this.maxValue = 96.0;
                this.translationKey = "terramath.config.base_height";
                this.setValue(settings.getBaseHeight());
            }
            case HEIGHT_VARIATION -> {
                this.minValue = 1.0;
                this.maxValue = 64.0;
                this.translationKey = "terramath.config.height_variation";
                this.setValue(settings.getHeightVariation());
            }
            case SMOOTHING_FACTOR -> {
                this.minValue = 0.1;
                this.maxValue = 1.0;
                this.translationKey = "terramath.config.smoothing";
                this.setValue(settings.getSmoothingFactor());
            }
            case COORDINATE_SCALE -> {
                this.minValue = 1.0;
                this.maxValue = 100.0;
                this.translationKey = "terramath.config.coordinate_scale";
                this.setValue(settings.getCoordinateScale());
            }
            case NOISE_SCALE_X -> {
                this.minValue = 0;
                this.maxValue = 60.0;
                this.translationKey = "terramath.config.noise_scale_x";
                this.setValue(settings.getNoiseScaleX());
            }
            case NOISE_SCALE_Y -> {
                this.minValue = 0;
                this.maxValue = 60.0;
                this.translationKey = "terramath.config.noise_scale_y";
                this.setValue(settings.getNoiseScaleY());
            }
            case NOISE_SCALE_Z -> {
                this.minValue = 0;
                this.maxValue = 60.0;
                this.translationKey = "terramath.config.noise_scale_z";
                this.setValue(settings.getNoiseScaleZ());
            }
            case NOISE_HEIGHT_SCALE -> {
                this.minValue = 0;
                this.maxValue = 30.0;
                this.translationKey = "terramath.config.noise_height_scale";
                this.setValue(settings.getNoiseHeightScale());
            }
            default -> throw new IllegalArgumentException("Unknown setting type: " + type);
        }
    }

    public void setOnValueChangeListener(Runnable listener) {
        this.onValueChangeListener = listener;
    }

    @Override
    protected void updateMessage() {
        double currentValue = getValue();
        String format = this.type == TerrainSettingType.SMOOTHING_FACTOR ? "%.2f" : "%.1f";
        this.setMessage(Component.translatable(this.translationKey).append(String.format(": " + format, currentValue)));
    }

    @Override
    protected void applyValue() {
        double newValue = getValue();
        this.settings.setTerrainSetting(this.type, newValue);
        this.updateMessage();

        if (this.onValueChangeListener != null) {
            this.onValueChangeListener.run();
        }
    }

    public double getDefaultValue() {
        return TerrainSettingsManager.getDefaultByType(this.type);
    }

    public void setValue(double realValue) {
        realValue = Mth.clamp(realValue, minValue, maxValue);

        this.value = (realValue - minValue) / (maxValue - minValue);
        this.updateMessage();
        this.applyValue();
    }

    public double getValue() {
        return Mth.lerp(this.value, minValue, maxValue);
    }

    public double getMinValue() {
        return minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public TerrainSettingType getType() {
        return type;
    }
}