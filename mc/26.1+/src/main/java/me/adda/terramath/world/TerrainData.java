package me.adda.terramath.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.adda.terramath.api.TerrainFormulaManager;
import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.api.TerrainSettingsManager.TerrainSettingType;
import me.adda.terramath.config.ModConfig;
import me.adda.terramath.config.NoiseType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public class TerrainData extends SavedData {
    public static final String DATA_ID = "terramath_terrain";

    private static final Codec<NoiseType> NOISE_TYPE_CODEC = Codec.STRING.xmap(
            name -> {
                try {
                    return NoiseType.valueOf(name);
                } catch (IllegalArgumentException e) {
                    return NoiseType.NONE;
                }
            },
            NoiseType::name
    );

    public static final Codec<TerrainData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("Formula", "").forGetter(data -> data.formula == null ? "" : data.formula),
            Codec.DOUBLE.optionalFieldOf("CoordinateScale", TerrainSettingsManager.getDefaultByType(TerrainSettingType.COORDINATE_SCALE)).forGetter(data -> data.coordinateScale),
            Codec.DOUBLE.optionalFieldOf("BaseHeight", TerrainSettingsManager.getDefaultByType(TerrainSettingType.BASE_HEIGHT)).forGetter(data -> data.baseHeight),
            Codec.DOUBLE.optionalFieldOf("HeightVariation", TerrainSettingsManager.getDefaultByType(TerrainSettingType.HEIGHT_VARIATION)).forGetter(data -> data.heightVariation),
            Codec.DOUBLE.optionalFieldOf("SmoothingFactor", TerrainSettingsManager.getDefaultByType(TerrainSettingType.SMOOTHING_FACTOR)).forGetter(data -> data.smoothingFactor),
            Codec.BOOL.optionalFieldOf("UseDensityMode", false).forGetter(data -> data.useDensityMode),
            NOISE_TYPE_CODEC.optionalFieldOf("NoiseType", NoiseType.NONE).forGetter(data -> data.noiseType),
            Codec.DOUBLE.optionalFieldOf("NoiseScaleX", TerrainSettingsManager.getDefaultByType(TerrainSettingType.NOISE_SCALE_X)).forGetter(data -> data.noiseScaleX),
            Codec.DOUBLE.optionalFieldOf("NoiseScaleY", TerrainSettingsManager.getDefaultByType(TerrainSettingType.NOISE_SCALE_Y)).forGetter(data -> data.noiseScaleY),
            Codec.DOUBLE.optionalFieldOf("NoiseScaleZ", TerrainSettingsManager.getDefaultByType(TerrainSettingType.NOISE_SCALE_Z)).forGetter(data -> data.noiseScaleZ),
            Codec.DOUBLE.optionalFieldOf("NoiseHeightScale", TerrainSettingsManager.getDefaultByType(TerrainSettingType.NOISE_HEIGHT_SCALE)).forGetter(data -> data.noiseHeightScale)
    ).apply(instance, TerrainData::new));

    public static final SavedDataType<TerrainData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("terramath", "terrain"),
            TerrainData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

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

    private TerrainData(String formula, double coordinateScale, double baseHeight, double heightVariation,
                        double smoothingFactor, boolean useDensityMode, NoiseType noiseType,
                        double noiseScaleX, double noiseScaleY, double noiseScaleZ, double noiseHeightScale) {
        this.formula = formula;
        this.coordinateScale = coordinateScale;
        this.baseHeight = baseHeight;
        this.heightVariation = heightVariation;
        this.smoothingFactor = smoothingFactor;
        this.useDensityMode = useDensityMode;
        this.noiseType = noiseType;
        this.noiseScaleX = noiseScaleX;
        this.noiseScaleY = noiseScaleY;
        this.noiseScaleZ = noiseScaleZ;
        this.noiseHeightScale = noiseHeightScale;
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