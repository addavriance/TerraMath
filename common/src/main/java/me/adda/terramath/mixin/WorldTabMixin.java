package me.adda.terramath.mixin;

import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.api.TerrainSettingsManager.TerrainSettingType;
import me.adda.terramath.api.FormulaCacheHolder;
import me.adda.terramath.api.TerraFormulaManager;
import me.adda.terramath.gui.ResetButton;
import me.adda.terramath.gui.TerrainSettingsSlider;
import me.adda.terramath.math.FormulaParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$WorldTab")
public abstract class WorldTabMixin extends GridLayoutTab {
    private final Minecraft minecraft = Minecraft.getInstance();
    private static final Component FORMULA_HINT = Component.translatable("terramath.worldgen.formula.hint")
            .withStyle(ChatFormatting.DARK_GRAY);
    private static final Component FORMULA_LABEL = Component.translatable("terramath.worldgen.formula");

    private static final Component CHECKBOX_LABEL = Component.translatable("terramath.worldgen.density_mode");

    private static final int BASE_WIDTH = 310;

    private static final int SPACING = 5;

    private static final int SLIDER_WIDTH = BASE_WIDTH - 20 - SPACING;

    private EditBox formulaField;
    private StringWidget errorWidget;
    private CycleButton<Boolean> densityModeCheckbox;
    private TerrainSettingsSlider coordinateScaleSlider;
    private TerrainSettingsSlider baseHeightSlider;
    private TerrainSettingsSlider heightVarSlider;
    private TerrainSettingsSlider smoothingSlider;

    private ResetButton coordinateScaleReset;
    private ResetButton baseHeightReset;
    private ResetButton heightVarReset;
    private ResetButton smoothingReset;

    public WorldTabMixin(Component component) {
        super(component);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        if (this.layout != null) {
            GridLayout.RowHelper rowHelper = this.layout.columnSpacing(10).rowSpacing(8).createRowHelper(2);

            for (int i = 0; i < 5; i++) {
                rowHelper.addChild(new GridLayout());
            }

            GridLayout formulaLayout = new GridLayout();
            formulaLayout.rowSpacing(4);
            GridLayout.RowHelper formulaRowHelper = formulaLayout.createRowHelper(1);

            formulaRowHelper.addChild(new StringWidget(FORMULA_LABEL, minecraft.font).alignLeft());

            this.formulaField = new EditBox(
                    minecraft.font,
                    0, 0,
                    BASE_WIDTH, 20,
                    Component.empty()
            ) {
                @Override
                protected MutableComponent createNarrationMessage() {
                    return super.createNarrationMessage().append(FORMULA_HINT);
                }
            };

            this.formulaField.setMaxLength(128);
            this.formulaField.setValue("");
            this.formulaField.setTextColor(0xFFFFFF);
            this.formulaField.setTextColorUneditable(0x808080);
            this.formulaField.setHint(FORMULA_HINT);
            this.formulaField.setResponder(this::onFormulaChanged);
            formulaRowHelper.addChild(this.formulaField);

            TerrainSettingsManager settings = TerrainSettingsManager.getInstance();

            this.errorWidget = new StringWidget(Component.empty(), minecraft.font).alignLeft();
            this.errorWidget.setWidth(BASE_WIDTH);
            formulaRowHelper.addChild(this.errorWidget);

            this.coordinateScaleSlider = new TerrainSettingsSlider(
                    0, 0, SLIDER_WIDTH,
                    TerrainSettingType.COORDINATE_SCALE,
                    settings
            );
            this.coordinateScaleSlider.setOnValueChangeListener(this::updateSlidersVisibility);

            this.coordinateScaleReset = new ResetButton(
                    SLIDER_WIDTH + SPACING, 0,
                    this.coordinateScaleSlider
            );

            GridLayout coordinateScaleLayout = new GridLayout();
            coordinateScaleLayout.columnSpacing(SPACING);
            GridLayout.RowHelper coordinateScaleRowHelper = coordinateScaleLayout.createRowHelper(2);
            coordinateScaleRowHelper.addChild(this.coordinateScaleSlider);
            coordinateScaleRowHelper.addChild(this.coordinateScaleReset);

            formulaRowHelper.addChild(coordinateScaleLayout);

            GridLayout densitySwitchLayout = new GridLayout().spacing(196);
            densitySwitchLayout.defaultCellSetting()
                    .alignHorizontallyCenter()
                    .alignVerticallyMiddle();

            GridLayout.RowHelper densitySwitchRowHelper = densitySwitchLayout.createRowHelper(2);

            StringWidget label = new StringWidget(CHECKBOX_LABEL, this.minecraft.font);
            label.alignLeft();
            label.setWidth(70);

            densitySwitchRowHelper.addChild(label);

            this.densityModeCheckbox = CycleButton.onOffBuilder(settings.isUseDensityMode())
                    .displayOnlyValue()
                    .create(0, 0, 45, 21, Component.empty(), (button, value) -> {
                        settings.setUseDensityMode(value);
                        updateSlidersVisibility();
                    });

            densitySwitchRowHelper.addChild(this.densityModeCheckbox);

            densitySwitchLayout.defaultCellSetting().paddingTop(4);

            formulaRowHelper.addChild(densitySwitchLayout);

            this.baseHeightSlider = new TerrainSettingsSlider(
                    0, 0, SLIDER_WIDTH,
                    TerrainSettingType.BASE_HEIGHT,
                    settings
            );
            this.baseHeightSlider.setOnValueChangeListener(this::updateSlidersVisibility);

            this.baseHeightReset = new ResetButton(
                    SLIDER_WIDTH + SPACING, 0,
                    this.baseHeightSlider
            );

            GridLayout baseHeightLayout = new GridLayout();
            baseHeightLayout.columnSpacing(SPACING);
            GridLayout.RowHelper baseHeightRowHelper = baseHeightLayout.createRowHelper(2);
            baseHeightRowHelper.addChild(this.baseHeightSlider);
            baseHeightRowHelper.addChild(this.baseHeightReset);

            baseHeightLayout.defaultCellSetting().paddingTop(5);

            this.heightVarSlider = new TerrainSettingsSlider(
                    0, 0, SLIDER_WIDTH,
                    TerrainSettingType.HEIGHT_VARIATION,
                    settings
            );
            this.heightVarSlider.setOnValueChangeListener(this::updateSlidersVisibility);

            this.heightVarReset = new ResetButton(
                    SLIDER_WIDTH + SPACING, 0,
                    this.heightVarSlider
            );

            GridLayout heightVarLayout = new GridLayout();
            heightVarLayout.columnSpacing(SPACING);
            GridLayout.RowHelper heightVarRowHelper = heightVarLayout.createRowHelper(2);
            heightVarRowHelper.addChild(this.heightVarSlider);
            heightVarRowHelper.addChild(this.heightVarReset);

            this.smoothingSlider = new TerrainSettingsSlider(
                    0, 0, SLIDER_WIDTH,
                    TerrainSettingType.SMOOTHING_FACTOR,
                    settings
            );
            this.smoothingSlider.setOnValueChangeListener(this::updateSlidersVisibility);

            this.smoothingReset = new ResetButton(
                    SLIDER_WIDTH + SPACING, 0,
                    this.smoothingSlider
            );

            GridLayout smoothingLayout = new GridLayout();
            smoothingLayout.columnSpacing(SPACING);
            GridLayout.RowHelper smoothingRowHelper = smoothingLayout.createRowHelper(2);
            smoothingRowHelper.addChild(this.smoothingSlider);
            smoothingRowHelper.addChild(this.smoothingReset);

            formulaRowHelper.addChild(baseHeightLayout);
            formulaRowHelper.addChild(heightVarLayout);
            formulaRowHelper.addChild(smoothingLayout);

            rowHelper.addChild(formulaLayout, 2);

            if (minecraft.screen instanceof CreateWorldScreen createWorldScreen) {
                createWorldScreen.getUiState().addListener((worldCreationUiState -> {
                    List<WorldCreationUiState.WorldTypeEntry> worldTypeList = worldCreationUiState.getNormalPresetList();
                    boolean isFirstWorldType = worldTypeList.indexOf(worldCreationUiState.getWorldType()) == 0;

                    this.formulaField.active = isFirstWorldType;
                    this.coordinateScaleSlider.active = isFirstWorldType;
                    this.coordinateScaleReset.active = isFirstWorldType;
                    this.densityModeCheckbox.active = isFirstWorldType;
                    this.baseHeightSlider.active = isFirstWorldType;
                    this.heightVarSlider.active = isFirstWorldType;
                    this.smoothingSlider.active = isFirstWorldType;
                    this.baseHeightReset.active = isFirstWorldType;
                    this.heightVarReset.active = isFirstWorldType;
                    this.smoothingReset.active = isFirstWorldType;
                }));
            }

            updateSlidersVisibility();
        }
    }

    private void onFormulaChanged(String formula) {
        Screen currentScreen = minecraft.screen;
        if (currentScreen instanceof CreateWorldScreen) {
            Button createButton = null;
            for (var child : currentScreen.children()) {
                if (child instanceof Button button &&
                        button.getMessage().contains(Component.translatable("selectWorld.create"))) {
                    createButton = button;
                    break;
                }
            }

            if (createButton != null) {
                FormulaParser.ValidationResult result = FormulaParser.validateFormula(formula);
                if (!result.isValid()) {
                    createButton.active = false;

                    Component errorMessage = Component.translatable(result.getErrorKey(), result.getErrorArgs())
                            .withStyle(ChatFormatting.RED);
                    errorWidget.setMessage(errorMessage);
                } else {
                    TerraFormulaManager.getInstance().setFormula(formula);

                    FormulaCacheHolder.resetCache();
                    createButton.active = true;
                    errorWidget.setMessage(Component.empty());
                }
            } else {
                if (errorWidget != null) {
                    errorWidget.setMessage(Component.empty());
                }
            }
        }
    }

    private void updateSlidersVisibility() {
        boolean visible = this.densityModeCheckbox.getValue();

        this.baseHeightSlider.visible = visible;
        this.heightVarSlider.visible = visible;
        this.smoothingSlider.visible = visible;
        this.baseHeightReset.visible = visible;
        this.heightVarReset.visible = visible;
        this.smoothingReset.visible = visible;
    }
}