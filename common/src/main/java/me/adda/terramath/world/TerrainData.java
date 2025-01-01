package me.adda.terramath.world;

import me.adda.terramath.api.TerraFormulaManager;
import me.adda.terramath.api.TerrainSettingsManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class TerrainData extends SavedData {
    public static final String DATA_ID = "terramath_terrain";

    public static Factory<TerrainData> factory() {
        return new Factory<>(
                TerrainData::new,
                TerrainData::load,
                DataFixTypes.LEVEL
        );
    }

    private String formula;
    private double coordinateScale;
    private double baseHeight;
    private double heightVariation;
    private double smoothingFactor;
    private boolean useDensityMode;
    private boolean isFirstLoad;

    public TerrainData() {
        this.formula = "";
        this.coordinateScale = 1.0;
        this.baseHeight = 64.0;
        this.heightVariation = 32.5;
        this.smoothingFactor = 0.55;
        this.useDensityMode = false;
        this.isFirstLoad = true;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putString("Formula", formula);
        tag.putDouble("CoordinateScale", coordinateScale);
        tag.putDouble("BaseHeight", baseHeight);
        tag.putDouble("HeightVariation", heightVariation);
        tag.putDouble("SmoothingFactor", smoothingFactor);
        tag.putBoolean("UseDensityMode", useDensityMode);
        tag.putBoolean("IsFirstLoad", isFirstLoad);
        return tag;
    }

    public static TerrainData load(CompoundTag tag) {
        TerrainData data = new TerrainData();
        if (tag != null) {
            data.formula = tag.getString("Formula");
            data.coordinateScale = tag.getDouble("CoordinateScale");
            data.baseHeight = tag.getDouble("BaseHeight");
            data.heightVariation = tag.getDouble("HeightVariation");
            data.smoothingFactor = tag.getDouble("SmoothingFactor");
            data.useDensityMode = tag.getBoolean("UseDensityMode");
            data.isFirstLoad = tag.getBoolean("IsFirstLoad");
        }
        return data;
    }

    public boolean isFirstLoad() {
        return isFirstLoad;
    }

    public void setFirstLoad(boolean firstLoad) {
        this.isFirstLoad = firstLoad;
        this.setDirty();
    }

    public void updateFromManagers() {
        this.formula = TerraFormulaManager.getInstance().getFormula();
        TerrainSettingsManager manager = TerrainSettingsManager.getInstance();
        this.coordinateScale = manager.getCoordinateScale();
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
        manager.setCoordinateScale(this.coordinateScale);
        manager.setUseDensityMode(this.useDensityMode);
    }
}