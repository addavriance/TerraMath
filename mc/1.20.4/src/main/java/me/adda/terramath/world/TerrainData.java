package me.adda.terramath.world;

import me.adda.terramath.api.TerrainFormulaManager;
import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.config.ModConfig;
import me.adda.terramath.config.NoiseType;
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

    private NoiseType noiseType;
    private double noiseScaleX;
    private double noiseScaleY;
    private double noiseScaleZ;
    private double noiseHeightScale;

    public TerrainData() {
        updateFromManagers();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putString("Formula", formula);
        tag.putDouble("CoordinateScale", coordinateScale);
        tag.putDouble("BaseHeight", baseHeight);
        tag.putDouble("HeightVariation", heightVariation);
        tag.putDouble("SmoothingFactor", smoothingFactor);
        tag.putBoolean("UseDensityMode", useDensityMode);

        tag.putString("NoiseType", noiseType.name());

        tag.putDouble("NoiseScaleX", noiseScaleX);
        tag.putDouble("NoiseScaleY", noiseScaleY);
        tag.putDouble("NoiseScaleZ", noiseScaleZ);
        tag.putDouble("NoiseHeightScale", noiseHeightScale);

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

            if (tag.contains("NoiseType")) {
                try {
                    data.noiseType = NoiseType.valueOf(tag.getString("NoiseType"));
                } catch (IllegalArgumentException e) {
                    data.noiseType = NoiseType.NONE;
                }

                data.noiseScaleX = tag.getDouble("NoiseScaleX");
                data.noiseScaleY = tag.getDouble( "NoiseScaleY");
                data.noiseScaleZ = tag.getDouble("NoiseScaleZ");
                data.noiseHeightScale = tag.getDouble("NoiseHeightScale");
            }
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

        this.noiseType = manager.getNoiseType();
        this.noiseScaleX = manager.getNoiseScaleX();
        this.noiseScaleY = manager.getNoiseScaleY();
        this.noiseScaleZ = manager.getNoiseScaleZ();
        this.noiseHeightScale = manager.getNoiseHeightScale();

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

        manager.setNoiseType(this.noiseType);
        manager.setNoiseScaleX(this.noiseScaleX);
        manager.setNoiseScaleY(this.noiseScaleY);
        manager.setNoiseScaleZ(this.noiseScaleZ);
        manager.setNoiseHeightScale(this.noiseHeightScale);
    }
}