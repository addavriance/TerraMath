package me.adda.terramath.gui;

import me.adda.terramath.api.TerrainSettingsManager;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class TerrainSettingsSlider extends AbstractSliderButton {
    private final double minValue;
    private final double maxValue;
    private final String translationKey;
    private final TerrainSettingsManager settings;
    private final TerrainSettingType type;

    public enum TerrainSettingType {
        BASE_HEIGHT,
        HEIGHT_VARIATION,
        SMOOTHING_FACTOR
    }

    public TerrainSettingsSlider(int x, int y, int width, TerrainSettingType type, TerrainSettingsManager settings) {
        super(x, y, width, 20, Component.empty(), 0.0);
        this.settings = settings;
        this.type = type;

        switch (type) {
            case BASE_HEIGHT -> {
                this.minValue = 32.0;
                this.maxValue = 96.0;
                this.translationKey = "terramath.worldgen.base_height";
                this.setValue(settings.getBaseHeight());
            }
            case HEIGHT_VARIATION -> {
                this.minValue = 1.0;
                this.maxValue = 64.0;
                this.translationKey = "terramath.worldgen.height_variation";
                this.setValue(settings.getHeightVariation());
            }
            case SMOOTHING_FACTOR -> {
                this.minValue = 0.1;
                this.maxValue = 1.0;
                this.translationKey = "terramath.worldgen.smoothing";
                this.setValue(settings.getSmoothingFactor());
            }
            default -> throw new IllegalArgumentException("Unknown setting type: " + type);
        }
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
    }

    public double getDefaultValue() {
        return switch (this.type) {
            case BASE_HEIGHT -> 64.0;
            case HEIGHT_VARIATION -> 32.5;
            case SMOOTHING_FACTOR -> 0.55;
        };
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