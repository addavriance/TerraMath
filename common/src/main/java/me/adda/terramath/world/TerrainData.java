package me.adda.terramath.world;

import me.adda.terramath.api.TerrainFormulaManager;
import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.config.ModConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

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

    public TerrainData() {
        updateFromManagers();
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putString("Formula", formula);
        tag.putDouble("CoordinateScale", coordinateScale);
        tag.putDouble("BaseHeight", baseHeight);
        tag.putDouble("HeightVariation", heightVariation);
        tag.putDouble("SmoothingFactor", smoothingFactor);
        tag.putBoolean("UseDensityMode", useDensityMode);

        return tag;
    }

    public static TerrainData load(CompoundTag tag, HolderLookup.Provider provider) {
        TerrainData data = new TerrainData();
        if (tag != null) {
            data.formula = tag.getString("Formula");
            data.coordinateScale = tag.getDouble("CoordinateScale");
            data.baseHeight = tag.getDouble("BaseHeight");
            data.heightVariation = tag.getDouble("HeightVariation");
            data.smoothingFactor = tag.getDouble("SmoothingFactor");
            data.useDensityMode = tag.getBoolean("UseDensityMode");
        }
        return data;
    }

    public void updateFromManagers() {
        TerrainFormulaManager formula_manager = TerrainFormulaManager.getInstance();
        TerrainSettingsManager manager = TerrainSettingsManager.getInstance();

        this.formula = formula_manager.getFormula();
        this.coordinateScale = manager.getCoordinateScale();
        this.baseHeight = manager.getBaseHeight();
        this.heightVariation = manager.getHeightVariation();
        this.smoothingFactor = manager.getSmoothingFactor();
        this.useDensityMode = manager.isUseDensityMode();

        if ((this.formula == null || this.formula.isEmpty()) && ModConfig.get().useDefaultFormula) {
            this.formula = ModConfig.get().baseFormula;
        }

        this.setDirty();
    }

    public void applyToManagers() {
        TerrainFormulaManager formula_manager = TerrainFormulaManager.getInstance();
        TerrainSettingsManager manager = TerrainSettingsManager.getInstance();

        formula_manager.setFormula(this.formula);
        manager.setCoordinateScale(this.coordinateScale);
        manager.setBaseHeight(this.baseHeight);
        manager.setHeightVariation(this.heightVariation);
        manager.setSmoothingFactor(this.smoothingFactor);
        manager.setUseDensityMode(this.useDensityMode);
    }
}