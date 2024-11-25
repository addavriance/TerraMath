package me.adda.terramath.world;

import net.minecraft.nbt.CompoundTag;

public class TerrainGenerationSettings {
    private String formula;
    private double baseHeight;
    private double heightVariation;
    private double smoothingFactor;
    private boolean useDensityMode;

    public TerrainGenerationSettings() {
        this.formula = "";
        this.baseHeight = 64.0;
        this.heightVariation = 32.5;
        this.smoothingFactor = 0.55;
        this.useDensityMode = false;
    }

    public void saveToNbt(CompoundTag tag) {
        CompoundTag terramath = new CompoundTag();
        terramath.putString("Formula", formula);
        terramath.putDouble("BaseHeight", baseHeight);
        terramath.putDouble("HeightVariation", heightVariation);
        terramath.putDouble("SmoothingFactor", smoothingFactor);
        terramath.putBoolean("UseDensityMode", useDensityMode);
        tag.put("TerraMath", terramath);
    }

    public void loadFromNbt(CompoundTag tag) {
        if (tag.contains("TerraMath")) {
            CompoundTag terramath = tag.getCompound("TerraMath");
            this.formula = terramath.getString("Formula");
            this.baseHeight = terramath.getDouble("BaseHeight");
            this.heightVariation = terramath.getDouble("HeightVariation");
            this.smoothingFactor = terramath.getDouble("SmoothingFactor");
            this.useDensityMode = terramath.getBoolean("UseDensityMode");
        }
    }

    public void copyFrom(TerrainGenerationSettings other) {
        this.formula = other.formula;
        this.baseHeight = other.baseHeight;
        this.heightVariation = other.heightVariation;
        this.smoothingFactor = other.smoothingFactor;
        this.useDensityMode = other.useDensityMode;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
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