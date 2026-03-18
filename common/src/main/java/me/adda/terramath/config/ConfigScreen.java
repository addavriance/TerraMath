package me.adda.terramath.config;

import me.adda.terramath.api.FormulaCacheHolder;
import me.adda.terramath.api.TerrainFormulaManager;
import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.api.TerrainSettingsManager.TerrainSettingType;
import me.adda.terramath.gui.PreviewPanel;
import me.adda.terramath.gui.ResetButton;
import me.adda.terramath.gui.TerrainSettingsSlider;
import me.adda.terramath.math.formula.LatexConverter;
import me.adda.terramath.math.parser.FormulaParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static me.adda.terramath.math.formula.FormulaGenerator.generateRandomFormula;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private final boolean isWorldCreation;

    // Scrollable list of settings
    private ConfigList configList;

    // UI components
    private Button saveButton;
    private Button cancelButton;

    // Layout for header, content, and footer
    private HeaderAndFooterLayout layout;

    // Original values for restoring on cancel
    private boolean originalDensityMode;
    private boolean originalUseDefault;

    SettingsDraft settingsDraft = null;

    static class SettingsDraft {
        String   formula;
        double   coordinateScale;
        boolean  densityMode;
        double   baseHeight;
        double   heightVariation;
        double   smoothingFactor;
        NoiseType noiseType;
        double   noiseScaleX, noiseScaleY, noiseScaleZ;
        double   noiseHeightScale;
        Boolean  useDefault; // null in world-creation mode
    }

    SettingsDraft captureCurrentDraft() {
        if (configList == null || !configList.hasEntries()) return null;
        SettingsDraft d = new SettingsDraft();
        d.formula         = configList.getFormula();
        d.coordinateScale = configList.getCoordinateScale();
        d.densityMode     = configList.getUseDensityMode();
        d.baseHeight      = configList.getBaseHeight();
        d.heightVariation = configList.getHeightVariation();
        d.smoothingFactor = configList.getSmoothingFactor();
        d.noiseType       = configList.getNoiseType();
        d.noiseScaleX     = configList.getNoiseScaleX();
        d.noiseScaleY     = configList.getNoiseScaleY();
        d.noiseScaleZ     = configList.getNoiseScaleZ();
        d.noiseHeightScale = configList.getNoiseHeightScale();
        d.useDefault      = isWorldCreation ? null : configList.useDefaultSelected();
        return d;
    }

    static int configHalfShift = 0;

    private final PreviewPanel previewPanel = new PreviewPanel();

    private int previewPanelX, previewPanelY, previewPanelW, previewPanelH;

    // UI constants
    private static final int FIELD_WIDTH = 300;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 8;
    private static final int ITEM_HEIGHT = 32;

    // Component labels
    private static final Component TITLE = Component.translatable("terramath.config.title");
    private static final Component FORMULA_LABEL = Component.translatable("terramath.config.formula");
    private static final Component FORMULA_HINT = Component.translatable("terramath.config.formula.hint")
            .withStyle(ChatFormatting.DARK_GRAY);
    private static final Component ADVANCED_LABEL = Component.translatable("terramath.config.density_mode");
    private static final Component USE_DEFAULT_LABEL = Component.translatable("terramath.config.use_default_settings");
    private static final Component RANDOM_SYMBOL = Component.literal("\uD83D\uDD00");
    private static final Component NOISE_SETTINGS_LABEL = Component.translatable("terramath.config.noise_settings");
    private static final Component TERRAIN_SETTINGS_LABEL = Component.translatable("terramath.config.terrain_settings");

    public ConfigScreen(Screen parent) {
        this(parent, false);
    }

    public ConfigScreen(Screen parent, boolean isWorldCreation) {
        super(TITLE);
        this.parent = parent;
        this.isWorldCreation = isWorldCreation;
        saveOriginalValues();
    }

    private void saveOriginalValues() {
        if (isWorldCreation) {
            // If in world creation screen, get values from managers
            TerrainSettingsManager settings = TerrainSettingsManager.getInstance();
            this.originalDensityMode = settings.isUseDensityMode();
            this.originalUseDefault = ModConfig.get().useDefaultFormula;
        } else {
            // Otherwise get values from config
            ModConfig config = ModConfig.get();
            this.originalDensityMode = config.useDensityMode;
            this.originalUseDefault = config.useDefaultFormula;
        }
    }

    @Override
    protected void init() {
        settingsDraft = captureCurrentDraft();

        super.init();

        // Create layout structure
        layout = new HeaderAndFooterLayout(this);

        // Add title to header
        layout.setHeaderHeight(44);
        layout.setFooterHeight(36);

        // Add title to header (FIXED VERSION)
        StringWidget titleWidget = new StringWidget(TITLE, this.font);
        layout.addToHeader(titleWidget, settings ->
                settings.alignHorizontally(0.5F).paddingTop(10));

        // Create config list for scrollable content
        configList = new ConfigList(minecraft, width, height, layout);
        layout.addToContents(configList, settings -> settings.paddingTop(0));

        // Add footer with buttons
        LinearLayout buttonLayout = layout.addToFooter(LinearLayout.horizontal().spacing(SPACING));

        // Add save and cancel buttons
        saveButton = buttonLayout.addChild(
                Button.builder(Component.translatable("gui.done"), button -> {
                    saveSettings();
                    this.minecraft.setScreen(parent);
                }).build()
        );

        buttonLayout.addChild(
                Button.builder(Component.translatable("gui.cancel"), button -> {
                    this.minecraft.setScreen(parent);
                }).build()
        );

        // Add all widgets
        layout.visitWidgets(this::addRenderableWidget);

        // Initialize values and arrange elements
        repositionElements();

        if (settingsDraft == null) {
            ModConfig cfg = ModConfig.get();
            previewPanel.halfWidth = Math.max(50, Math.min(400, cfg.previewHalfWidth));
            previewPanel.zoom      = Math.max(0.5f, Math.min(4.0f, cfg.previewZoom));
        }
    }

    @Override
    protected void repositionElements() {
        if (configList != null) {
            SettingsDraft tempDraft = captureCurrentDraft();
            if (tempDraft != null) settingsDraft = tempDraft;

            computePreviewGeometry();

            configList.setSize(
                    width,
                    height - layout.getHeaderHeight() - layout.getFooterHeight()
            );
            configList.refreshEntries();
            previewPanel.setDisplaySettings(configList.displaySettings);
        }
        layout.arrangeElements();
    }

    private void computePreviewGeometry() {
        int configFootprintHalf = FIELD_WIDTH / 2 + 26; // = 176

        int shift = width / 4;

        int configRightEdge = (width / 2 - shift) + configFootprintHalf;

        int pX = configRightEdge + 10;
        int pW = width - pX - 8;

        int configLeftEdge = (width / 2 - shift) - configFootprintHalf;
        if (configLeftEdge < 8 || pW < 130) {
            configHalfShift = 0;
            previewPanelW = 0;
            return;
        }

        configHalfShift = shift;
        previewPanelX = pX;
        previewPanelW = pW;
        previewPanelY = layout.getHeaderHeight() + 4;
        previewPanelH = height - previewPanelY - layout.getFooterHeight() - 4;

        if (previewPanelH < 60) {
            configHalfShift = 0;
            previewPanelW = 0;
        }
    }

    private void saveSettings() {
        ModConfig.get().previewHalfWidth = previewPanel.halfWidth;
        ModConfig.get().previewZoom      = previewPanel.zoom;

        String formula = configList.getFormula();
        boolean useDensityMode = configList.getUseDensityMode();
        double coordinateScale = configList.getCoordinateScale();
        double baseHeight = configList.getBaseHeight();
        double heightVariation = configList.getHeightVariation();
        double smoothingFactor = configList.getSmoothingFactor();

        NoiseType noiseType = configList.getNoiseType();
        double noiseScaleX = configList.getNoiseScaleX();
        double noiseScaleY = configList.getNoiseScaleY();
        double noiseScaleZ = configList.getNoiseScaleZ();
        double noiseHeightScale = configList.getNoiseHeightScale();

        boolean useByDefault = configList.useDefaultSelected();

        if (isWorldCreation) {
            TerrainSettingsManager settings = TerrainSettingsManager.getInstance();

            TerrainFormulaManager.getInstance().setFormula(formula);

            settings.setUseDensityMode(useDensityMode);
            settings.setCoordinateScale(coordinateScale);
            settings.setBaseHeight(baseHeight);
            settings.setHeightVariation(heightVariation);
            settings.setSmoothingFactor(smoothingFactor);

            settings.setNoiseType(noiseType);
            settings.setNoiseScaleX(noiseScaleX);
            settings.setNoiseScaleY(noiseScaleY);
            settings.setNoiseScaleZ(noiseScaleZ);
            settings.setNoiseHeightScale(noiseHeightScale);

            FormulaCacheHolder.resetCache();
            ModConfig.save();
        } else {
            ModConfig config = ModConfig.get();

            config.baseFormula = formula;
            config.useDensityMode = useDensityMode;
            config.coordinateScale = coordinateScale;
            config.baseHeight = baseHeight;
            config.heightVariation = heightVariation;
            config.smoothingFactor = smoothingFactor;

            config.noiseType = noiseType;
            config.noiseScaleX = noiseScaleX;
            config.noiseScaleY = noiseScaleY;
            config.noiseScaleZ = noiseScaleZ;
            config.noiseHeightScale = noiseHeightScale;

            config.useDefaultFormula = useByDefault;

            ModConfig.save();

            if (Minecraft.getInstance().level == null) {
                TerrainFormulaManager.getInstance().setFormula("");
                TerrainSettingsManager.getInstance().resetToDefaults();

                if (config.useDefaultFormula) {
                    ModConfig.updateTerrainSettingsFromConfig();
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (previewPanelW > 0) {
            previewPanel.render(graphics, previewPanelX, previewPanelY, previewPanelW, previewPanelH, this.font);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (previewPanelW > 0 && previewPanel.mouseClicked(mouseX, mouseY, previewPanelX, previewPanelY, previewPanelW, previewPanelH, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (previewPanelW > 0 && previewPanel.mouseDragged(mouseX, mouseY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        previewPanel.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    class ConfigList extends ContainerObjectSelectionList<ConfigEntry> {
        private final TerrainSettingsManager displaySettings = new TerrainSettingsManager();

        // Store the entry objects to access their values later
        private FormulaEntry formulaEntry;
        private DensityModeEntry densityModeEntry;
        private SliderEntry coordinateScaleEntry;
        private SliderEntry baseHeightEntry;
        private SliderEntry heightVarEntry;
        private SliderEntry smoothingEntry;
        private UseDefaultEntry useDefaultEntry;
        private NoiseTypeEntry noiseTypeEntry;
        private SliderEntry noiseScaleXEntry;
        private SliderEntry noiseScaleYEntry;
        private SliderEntry noiseScaleZEntry;
        private SliderEntry noiseHeightScaleEntry;

        public ConfigList(Minecraft minecraft, int width, int height, HeaderAndFooterLayout layout) {
            super(
                    minecraft,
                    width,
                    height - layout.getHeaderHeight() - layout.getFooterHeight(),
                    layout.getHeaderHeight(),
                    ITEM_HEIGHT
            );
        }

        @Override
        public void setSize(int width, int height) {
            super.setSize(width, height);
            this.setX(-ConfigScreen.configHalfShift);
        }

        @Override
        public int getRowWidth() {
            return FIELD_WIDTH + 40;
        }

        @Override
        public int getRowLeft() {
            return this.width / 2 - ConfigScreen.configHalfShift - getRowWidth() / 2;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.width / 2 - ConfigScreen.configHalfShift + FIELD_WIDTH / 2 + 14;
        }

        boolean hasEntries() { return formulaEntry != null; }

        public void refreshEntries() {
            this.clearEntries();

            // Add all config entries in the desired order

            // Formula section
            formulaEntry = new FormulaEntry();
            this.addEntry(formulaEntry);

            // Terrain settings section
            this.addEntry(new CategoryEntry(TERRAIN_SETTINGS_LABEL, 15));

            coordinateScaleEntry = new SliderEntry(TerrainSettingType.COORDINATE_SCALE, displaySettings);
            this.addEntry(coordinateScaleEntry);

            densityModeEntry = new DensityModeEntry(originalDensityMode);
            this.addEntry(densityModeEntry);

            baseHeightEntry = new SliderEntry(TerrainSettingType.BASE_HEIGHT, displaySettings);
            this.addEntry(baseHeightEntry);

            heightVarEntry = new SliderEntry(TerrainSettingType.HEIGHT_VARIATION, displaySettings);
            this.addEntry(heightVarEntry);

            smoothingEntry = new SliderEntry(TerrainSettingType.SMOOTHING_FACTOR, displaySettings);
            this.addEntry(smoothingEntry);

            // Noise settings section
            this.addEntry(new CategoryEntry(NOISE_SETTINGS_LABEL));

            noiseTypeEntry = new NoiseTypeEntry();
            this.addEntry(noiseTypeEntry);

            noiseScaleXEntry = new SliderEntry(TerrainSettingType.NOISE_SCALE_X, displaySettings);
            this.addEntry(noiseScaleXEntry);

            noiseScaleYEntry = new SliderEntry(TerrainSettingType.NOISE_SCALE_Y, displaySettings);
            this.addEntry(noiseScaleYEntry);

            noiseScaleZEntry = new SliderEntry(TerrainSettingType.NOISE_SCALE_Z, displaySettings);
            this.addEntry(noiseScaleZEntry);

            noiseHeightScaleEntry = new SliderEntry(TerrainSettingType.NOISE_HEIGHT_SCALE, displaySettings);
            this.addEntry(noiseHeightScaleEntry);

            if (!isWorldCreation) {
                useDefaultEntry = new UseDefaultEntry(originalUseDefault);
                this.addEntry(useDefaultEntry);
            }

            // Load initial values
            loadValuesForDisplay();
        }

        private void loadValuesForDisplay() {
            if (settingsDraft != null) {
                formulaEntry.setFormula(settingsDraft.formula);
                coordinateScaleEntry.setValue(settingsDraft.coordinateScale);
                baseHeightEntry.setValue(settingsDraft.baseHeight);
                heightVarEntry.setValue(settingsDraft.heightVariation);
                smoothingEntry.setValue(settingsDraft.smoothingFactor);
                noiseTypeEntry.setValue(settingsDraft.noiseType);
                noiseScaleXEntry.setValue(settingsDraft.noiseScaleX);
                noiseScaleYEntry.setValue(settingsDraft.noiseScaleY);
                noiseScaleZEntry.setValue(settingsDraft.noiseScaleZ);
                noiseHeightScaleEntry.setValue(settingsDraft.noiseHeightScale);
                if (densityModeEntry != null) {
                    densityModeEntry.setValue(settingsDraft.densityMode);
                    displaySettings.setUseDensityMode(settingsDraft.densityMode);
                }
                return;
            }

            // First load — read from persisted managers / config
            if (isWorldCreation) {
                TerrainSettingsManager settings = TerrainSettingsManager.getInstance();
                formulaEntry.setFormula(TerrainFormulaManager.getInstance().getFormula());
                coordinateScaleEntry.setValue(settings.getCoordinateScale());
                baseHeightEntry.setValue(settings.getBaseHeight());
                heightVarEntry.setValue(settings.getHeightVariation());
                smoothingEntry.setValue(settings.getSmoothingFactor());
                noiseTypeEntry.setValue(settings.getNoiseType());
                noiseScaleXEntry.setValue(settings.getNoiseScaleX());
                noiseScaleYEntry.setValue(settings.getNoiseScaleY());
                noiseScaleZEntry.setValue(settings.getNoiseScaleZ());
                noiseHeightScaleEntry.setValue(settings.getNoiseHeightScale());
                if (densityModeEntry != null) {
                    densityModeEntry.setValue(settings.isUseDensityMode());
                    displaySettings.setUseDensityMode(settings.isUseDensityMode());
                }
            } else {
                ModConfig config = ModConfig.get();
                formulaEntry.setFormula(config.baseFormula);
                coordinateScaleEntry.setValue(config.coordinateScale);
                baseHeightEntry.setValue(config.baseHeight);
                heightVarEntry.setValue(config.heightVariation);
                smoothingEntry.setValue(config.smoothingFactor);
                noiseTypeEntry.setValue(config.noiseType);
                noiseScaleXEntry.setValue(config.noiseScaleX);
                noiseScaleYEntry.setValue(config.noiseScaleY);
                noiseScaleZEntry.setValue(config.noiseScaleZ);
                noiseHeightScaleEntry.setValue(config.noiseHeightScale);
                if (densityModeEntry != null) {
                    densityModeEntry.setValue(config.useDensityMode);
                    displaySettings.setUseDensityMode(config.useDensityMode);
                }
            }

            // Update visibility based on density mode
            updateAdvancedVisibility(densityModeEntry.getValue());
            updateNoiseVisibility(noiseTypeEntry.getValue());
        }

        public void updateAdvancedVisibility(boolean useDensityMode) {
            baseHeightEntry.setVisible(useDensityMode);
            heightVarEntry.setVisible(useDensityMode);
            smoothingEntry.setVisible(useDensityMode);
        }

        public void updateNoiseVisibility(NoiseType noiseType) {
            boolean isSimplex = noiseType == NoiseType.SIMPLEX;
            boolean isNone = noiseType == NoiseType.NONE;

            noiseScaleXEntry.setVisible(!isNone);
            noiseScaleYEntry.setVisible(!isNone && !isSimplex);
            noiseScaleZEntry.setVisible(!isNone);
            noiseHeightScaleEntry.setVisible(!isNone);

        }

        // Getters for all settings when saving
        public String getFormula() {
            return formulaEntry.getFormula();
        }

        public boolean getUseDensityMode() {
            return densityModeEntry.getValue();
        }

        public double getCoordinateScale() {
            return coordinateScaleEntry.getValue();
        }

        public double getBaseHeight() {
            return baseHeightEntry.getValue();
        }

        public double getHeightVariation() {
            return heightVarEntry.getValue();
        }

        public double getSmoothingFactor() {
            return smoothingEntry.getValue();
        }

        public boolean useDefaultSelected() {
            return useDefaultEntry != null && useDefaultEntry.isSelected();
        }

        public NoiseType getNoiseType() {
            return noiseTypeEntry.getValue();
        }

        public double getNoiseScaleX() {
            return noiseScaleXEntry.getValue();
        }

        public double getNoiseScaleY() {
            return noiseScaleYEntry.getValue();
        }

        public double getNoiseScaleZ() {
            return noiseScaleZEntry.getValue();
        }

        public double getNoiseHeightScale() {
            return noiseHeightScaleEntry.getValue();
        }
    }

    // Base class for entries in the config list
    abstract static class ConfigEntry extends ContainerObjectSelectionList.Entry<ConfigEntry> {
        protected final List<GuiEventListener> children = new ArrayList<>();
        protected boolean visible = true;

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return children.stream()
                    .filter(child -> child instanceof NarratableEntry)
                    .map(child -> (NarratableEntry) child)
                    .toList();
        }
    }

    // Entry for section headers
    class CategoryEntry extends ConfigEntry {
        private final Component label;
        private final int offsetY;

        public CategoryEntry(Component label) {
            this.label = label;
            this.offsetY = 0;
        }

        public CategoryEntry(Component label, int offsetY) {
            this.label = label;
            this.offsetY = offsetY;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (!visible) return;

            int centerX = left + width / 2;
            graphics.drawCenteredString(font, label, centerX, top + 5 + offsetY, 0xFFFFFF);
        }
    }

    // Entry for formula input
    class FormulaEntry extends ConfigEntry {
        private final StringWidget label;
        private final EditBox formulaField;
        private final Button randomButton;
        private final Button swapButton;
        private final StringWidget errorWidget;

        public FormulaEntry() {
            int centerX = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 - ConfigScreen.configHalfShift;

            int smallButtonWidth = 20;
            int smallButtonMargin = 4;
            int totalWidth = FIELD_WIDTH;

            int fieldWidth = totalWidth - 2 * (smallButtonWidth + smallButtonMargin);

            int fieldX = centerX - totalWidth / 2;
            int randomButtonX = fieldX + fieldWidth + smallButtonMargin;
            int swapButtonX = randomButtonX + smallButtonWidth + smallButtonMargin;

            label = new StringWidget(FORMULA_LABEL, minecraft.font).alignLeft();
            label.setWidth(FIELD_WIDTH);
            label.setX(centerX - FIELD_WIDTH / 2);
            label.setY(5);

            formulaField = new EditBox(
                    minecraft.font, fieldX, 15,
                    fieldWidth, 20, Component.empty()
            ) {
                @Override
                protected MutableComponent createNarrationMessage() {
                    return super.createNarrationMessage().append(FORMULA_HINT);
                }

                @Override
                public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                    if (Screen.isPaste(keyCode)) {
                        String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
                        if (clipboard != null && LatexConverter.isLatex(clipboard.trim())) {
                            String converted = LatexConverter.convert(clipboard.trim());
                            setValue(converted);
                            onFormulaChanged(converted);
                            return true;
                        }
                    }
                    return super.keyPressed(keyCode, scanCode, modifiers);
                }
            };

            formulaField.setMaxLength(1024);
            formulaField.setTextColor(0xFFFFFF);
            formulaField.setTextColorUneditable(0x808080);
            formulaField.setHint(FORMULA_HINT);
            formulaField.setResponder(this::onFormulaChanged);

            // Random formula button
            randomButton = Button.builder(
                    RANDOM_SYMBOL,
                    button -> {
                        formulaField.setValue(generateRandomFormula());
                        onFormulaChanged(formulaField.getValue());
                    }
            ).pos(randomButtonX, 15).size(20, 20).build();

            swapButton = Button.builder(
                    Component.literal("⇄"),
                    button -> {
                        String swapped = LatexConverter.swapZAndY(formulaField.getValue());
                        formulaField.setValue(swapped);
                        onFormulaChanged(swapped);
                    }
            ).pos(swapButtonX, 15).size(20, 20).build();
            swapButton.setTooltip(Tooltip.create(
                    Component.translatable("terramath.config.swap_zy.tooltip")));

            // Error message widget
            errorWidget = new StringWidget(Component.empty(), minecraft.font).alignLeft();
            errorWidget.setWidth(FIELD_WIDTH);
            errorWidget.setX(centerX - FIELD_WIDTH / 2);
            errorWidget.setY(38);

            children.add(label);
            children.add(formulaField);
            children.add(randomButton);
            children.add(swapButton);
            children.add(errorWidget);
        }

        private void onFormulaChanged(String formula) {
            previewPanel.setFormula(formula != null ? formula : "");
            if (formula == null || formula.trim().isEmpty()) {
                errorWidget.setMessage(Component.empty());
                saveButton.active = true;
                return;
            }

            FormulaParser.ValidationResult result = FormulaParser.validateFormula(formula, false);
            if (!result.isValid()) {
                saveButton.active = false;

                Component errorMessage = Component.translatable(result.errorKey(), result.errorArgs())
                        .withStyle(ChatFormatting.RED);
                errorWidget.setMessage(errorMessage);
            } else {
                saveButton.active = true;
                errorWidget.setMessage(Component.empty());
            }
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (!visible) return;

            label.setY(top + 5);
            formulaField.setY(top + 15);
            randomButton.setY(top + 15);
            swapButton.setY(top + 15);
            errorWidget.setY(top + 38);

            label.render(graphics, mouseX, mouseY, partialTick);
            formulaField.render(graphics, mouseX, mouseY, partialTick);
            randomButton.render(graphics, mouseX, mouseY, partialTick);
            swapButton.render(graphics, mouseX, mouseY, partialTick);
            errorWidget.render(graphics, mouseX, mouseY, partialTick);

            String errorText = errorWidget.getMessage().getString();
            Font font = Minecraft.getInstance().font;

            int textWidth = font.width(errorText);
            int widgetWidth = errorWidget.getWidth();

            if (errorWidget.isHovered() && textWidth > widgetWidth) {
                List<Component> tooltipLines = List.of(
                        Component.literal("").append(errorWidget.getMessage()).withStyle(ChatFormatting.RED)
                );

                graphics.renderTooltip(
                        Minecraft.getInstance().font,
                        tooltipLines,
                        Optional.empty(),
                        mouseX,
                        mouseY
                );
            }
        }

        public String getFormula() {
            return formulaField.getValue().trim();
        }

        public void setFormula(String formula) {
            formulaField.setValue(formula);
            onFormulaChanged(formula);
        }

    }

    // Entry for density mode checkbox
    class DensityModeEntry extends ConfigEntry {
        private CycleButton<Boolean> checkbox;

        public DensityModeEntry(boolean initialValue) {
            int centerX = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2  - ConfigScreen.configHalfShift;

            checkbox = CycleButton.onOffBuilder(initialValue)
                    .displayOnlyValue()
                    .create(centerX + FIELD_WIDTH / 2 - 45, 5, 45, 20, ADVANCED_LABEL, (button, value) -> {
                        configList.updateAdvancedVisibility(value);
                        configList.displaySettings.setUseDensityMode(value);
                    });

            children.add(checkbox);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (!visible) return;

            int centerX = left + width / 2;

            graphics.drawString(font, ADVANCED_LABEL, centerX - FIELD_WIDTH / 2, top + 10, 0xFFFFFF);

            checkbox.setY(top + 5);
            checkbox.render(graphics, mouseX, mouseY, partialTick);
        }

        public boolean getValue() {
            return checkbox.getValue();
        }

        public void setValue(boolean value) {
            checkbox.setValue(value);
        }
    }

    // Entry for slider settings
    static class SliderEntry extends ConfigEntry {
        private TerrainSettingsSlider slider;
        private ResetButton resetButton;
        private final TerrainSettingType settingType;

        public SliderEntry(TerrainSettingType settingType, TerrainSettingsManager displaySettings) {
            this.settingType = settingType;
            int centerX = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 - ConfigScreen.configHalfShift;

            int resetButtonWidth = 20;
            int resetButtonMargin = 4;
            int totalWidth = FIELD_WIDTH;

            int sliderWidth = totalWidth - resetButtonWidth - resetButtonMargin;

            int sliderX = centerX - totalWidth / 2;

            int buttonX = sliderX + sliderWidth + resetButtonMargin;

            slider = new TerrainSettingsSlider(
                    sliderX, 5, sliderWidth,
                    settingType, displaySettings
            );

            resetButton = new ResetButton(
                    buttonX, 5, slider
            );

            children.add(slider);
            children.add(resetButton);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (!visible) {
                mouseX = -1;
                mouseY = -1;

                slider.setAlpha(0.5f);
                resetButton.setAlpha(0.5f);

                slider.active = false;
                resetButton.active = false;
            } else {
                slider.setAlpha(1f);
                resetButton.setAlpha(1f);

                slider.active = true;
                resetButton.active = true;
            }

            slider.setY(top + 5);
            resetButton.setY(top + 5);

            slider.render(graphics, mouseX, mouseY, partialTick);
            resetButton.render(graphics, mouseX, mouseY, partialTick);
        }

        public double getValue() {
            return slider.getValue();
        }

        public void setValue(double value) {
            slider.setValue(value);
        }

        public TerrainSettingType getSettingType() {
            return settingType;
        }
    }

    // Entry for "use default" checkbox
    class UseDefaultEntry extends ConfigEntry {
        private Checkbox checkbox;

        public UseDefaultEntry(boolean initialValue) {
            int centerX = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 - ConfigScreen.configHalfShift;

            checkbox = Checkbox.builder(USE_DEFAULT_LABEL, minecraft.font)
                    .pos(centerX - FIELD_WIDTH / 2, 5)
                    .selected(initialValue)
                    .build();

            children.add(checkbox);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (!visible) return;

            checkbox.setY(top + 5);
            checkbox.render(graphics, mouseX, mouseY, partialTick);
        }

        public boolean isSelected() {
            return checkbox.selected();
        }
    }

    // Entry for noise type selection
    class NoiseTypeEntry extends ConfigEntry {
        private final CycleButton<NoiseType> cycleButton;

        public NoiseTypeEntry() {
            int centerX = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2 - ConfigScreen.configHalfShift;

            cycleButton = CycleButton.<NoiseType>builder(NoiseType::getDisplayName)
                    .withValues(NoiseType.values())
                    .withInitialValue(NoiseType.NONE)
                    .create(
                            centerX - FIELD_WIDTH / 2, 5,
                            FIELD_WIDTH, 20,
                            Component.translatable("terramath.config.noise_type"),
                            (button, noiseType) -> {
                                configList.updateNoiseVisibility(noiseType);
                                configList.displaySettings.setNoiseType(noiseType);
                            }
                    );

            children.add(cycleButton);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (!visible) return;

            cycleButton.setY(top);
            cycleButton.render(graphics, mouseX, mouseY, partialTick);
        }

        public NoiseType getValue() {
            return cycleButton.getValue();
        }

        public void setValue(NoiseType value) {
            cycleButton.setValue(value);
            configList.displaySettings.setNoiseType(value);
        }
    }

}
