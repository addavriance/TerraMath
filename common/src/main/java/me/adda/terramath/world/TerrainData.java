package me.adda.terramath.world;

import dev.architectury.platform.Platform;
import me.adda.terramath.api.TerraFormulaManager;
import me.adda.terramath.api.TerrainSettingsManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;

public class TerrainData extends SavedData {
    private static final String DATA_ID = "terramath_terrain";
    public static final ResourceLocation IDENTIFIER = new ResourceLocation("terramath", DATA_ID);

    public String formula;
    public boolean isTerraMathWorld;
    private double baseHeight;
    private double heightVariation;
    private double smoothingFactor;
    private boolean useDensityMode;

    public TerrainData() {
        this.formula = "";
        this.isTerraMathWorld = false;
        this.baseHeight = 64.0;
        this.heightVariation = 32.5;
        this.smoothingFactor = 0.55;
        this.useDensityMode = false;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("IsTerraMathWorld", isTerraMathWorld);
        tag.putString("Formula", formula);
        tag.putDouble("BaseHeight", baseHeight);
        tag.putDouble("HeightVariation", heightVariation);
        tag.putDouble("SmoothingFactor", smoothingFactor);
        tag.putBoolean("UseDensityMode", useDensityMode);
        return tag;
    }

    public static TerrainData load(CompoundTag tag) {
        TerrainData data = new TerrainData();
        if (tag != null) {
            data.isTerraMathWorld = tag.getBoolean("IsTerraMathWorld");
            data.formula = tag.getString("Formula");
            data.baseHeight = tag.getDouble("BaseHeight");
            data.heightVariation = tag.getDouble("HeightVariation");
            data.smoothingFactor = tag.getDouble("SmoothingFactor");
            data.useDensityMode = tag.getBoolean("UseDensityMode");
        }
        return data;
    }

    public void updateFromManagers() {
        this.formula = TerraFormulaManager.getInstance().getFormula();

        TerrainSettingsManager manager = TerrainSettingsManager.getInstance();

        this.baseHeight = manager.getBaseHeight();
        this.heightVariation = manager.getHeightVariation();
        this.smoothingFactor = manager.getSmoothingFactor();
        this.useDensityMode = manager.isUseDensityMode();
        this.setDirty();
    }

    public void applyToManagers() {
        TerraFormulaManager.getInstance().setFormula(this.formula);
        TerrainSettingsManager manager = TerrainSettingsManager.getInstance();
        manager.setBaseHeight(this.baseHeight);
        manager.setHeightVariation(this.heightVariation);
        manager.setSmoothingFactor(this.smoothingFactor);
        manager.setUseDensityMode(this.useDensityMode);
    }
}