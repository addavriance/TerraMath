package me.adda.terramath.config;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private EditBox formulaField;
    private Checkbox useDefaultCheckbox;
    private Checkbox customWorldHeightCheckbox;
    private Checkbox symmetricHeightCheckbox;
    private EditBox worldHeightField;

    // Temp values to store until save
    private String tempFormula;
    private final boolean tempUseDefault;
    private final boolean tempCustomWorldHeight;
    private final int tempWorldHeight;
    private final boolean tempSymmetricHeight;

    // UI constants
    private static final int FIELD_WIDTH = 200;
    private static final int BUTTON_WIDTH = 96;
    private static final int BUTTON_HEIGHT = 20;
    private static final int FIELD_HEIGHT = 20;
    private static final int SPACING = 6;
    private static final int LINE_HEIGHT = 24;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("config.terramath.title"));
        this.parent = parent;

        this.tempFormula = ModConfig.get().baseFormula;
        this.tempUseDefault = ModConfig.get().useDefaultFormula;
        this.tempCustomWorldHeight = ModConfig.get().customWorldHeight;
        this.tempWorldHeight = ModConfig.get().worldHeight;
        this.tempSymmetricHeight = ModConfig.get().symmetricHeight;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = 50;
        int currentY = startY;

        currentY += 10;

        this.formulaField = new EditBox(this.font, centerX - FIELD_WIDTH/2, currentY, FIELD_WIDTH, FIELD_HEIGHT, Component.empty());
        this.formulaField.setValue(tempFormula);
        this.formulaField.setMaxLength(128);
        this.addRenderableWidget(this.formulaField);

        currentY += FIELD_HEIGHT + SPACING;

        Button generateButton = Button.builder(Component.translatable("config.terramath.generate"), button -> {
            tempFormula = ModConfig.generateRandomFormula();
            formulaField.setValue(tempFormula);
        }).pos(centerX - BUTTON_WIDTH - SPACING/2, currentY).size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(generateButton);

        Button previewButton = Button.builder(Component.translatable("config.terramath.preview"), button -> {
            // Preview formula logic would go here. Maybe...
        }).pos(centerX + SPACING/2, currentY).size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(previewButton);

        currentY += BUTTON_HEIGHT + SPACING;

        this.useDefaultCheckbox = Checkbox.builder(
                Component.translatable("config.terramath.useDefault"),
                this.minecraft.font
        ).pos(centerX - FIELD_WIDTH/2, currentY).selected(tempUseDefault).build();
        this.addRenderableWidget(this.useDefaultCheckbox);

        currentY += LINE_HEIGHT + 10;

        this.customWorldHeightCheckbox = Checkbox.builder(
                Component.translatable("config.terramath.customWorldHeight"),
                this.minecraft.font
        ).pos(centerX - FIELD_WIDTH/2, currentY).selected(tempCustomWorldHeight).onValueChange((element, value) -> {
            worldHeightField.setEditable(value);
            worldHeightField.setFocused(value);
            symmetricHeightCheckbox.active = value;
            worldHeightField.moveCursorToStart(value);
        }).build();
        this.addRenderableWidget(this.customWorldHeightCheckbox);

        currentY += LINE_HEIGHT;

        this.worldHeightField = new EditBox(this.font, centerX - FIELD_WIDTH/2, currentY, 80, FIELD_HEIGHT, Component.empty());
        this.worldHeightField.setValue(String.valueOf(tempWorldHeight));
        this.worldHeightField.setMaxLength(4);
        this.worldHeightField.setFilter(this::isValidNumberInput);
        this.worldHeightField.setEditable(tempCustomWorldHeight);
        this.addRenderableWidget(this.worldHeightField);

        currentY += FIELD_HEIGHT + SPACING;

        this.symmetricHeightCheckbox = Checkbox.builder(
                Component.translatable("config.terramath.symmetricHeight"),
                this.minecraft.font
        ).pos(centerX - FIELD_WIDTH/2, currentY).selected(tempSymmetricHeight).build();
        this.symmetricHeightCheckbox.active = tempCustomWorldHeight;
        this.addRenderableWidget(this.symmetricHeightCheckbox);

        int bottomY = this.height - 30;

        Button cancelButton = Button.builder(Component.translatable("gui.cancel"), button -> {
            this.minecraft.setScreen(parent);
        }).pos(centerX - BUTTON_WIDTH - SPACING, bottomY).size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(cancelButton);

        Button saveButton = Button.builder(Component.translatable("gui.done"), button -> {
            saveConfig();
            this.minecraft.setScreen(parent);
        }).pos(centerX + SPACING, bottomY).size(BUTTON_WIDTH, BUTTON_HEIGHT).build();
        this.addRenderableWidget(saveButton);
    }

    private boolean isValidNumberInput(String text) {
        if (text.isEmpty()) return true;
        try {
            int value = Integer.parseInt(text);
            return value >= 0 && value <= 2048;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void saveConfig() {
        ModConfig config = ModConfig.get();
        config.baseFormula = this.formulaField.getValue();
        config.useDefaultFormula = this.useDefaultCheckbox.selected();
        config.customWorldHeight = this.customWorldHeightCheckbox.selected();
        config.symmetricHeight = this.symmetricHeightCheckbox.selected();

        try {
            if (!this.worldHeightField.getValue().isEmpty()) {
                config.worldHeight = Integer.parseInt(this.worldHeightField.getValue());
            }
        } catch (NumberFormatException e) {
            // Keep previous value if invalid input
        }

        ModConfig.save();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, 0, 0, 0.5f);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        guiGraphics.drawString(this.font, Component.translatable("config.terramath.formulaLabel"),
                this.formulaField.getX(), this.formulaField.getY() - 12, 0xFFFFFF);

        if (this.customWorldHeightCheckbox.selected()) {
            guiGraphics.drawString(this.font, Component.translatable("config.terramath.heightRange", "64-2048"),
                    this.worldHeightField.getX() + this.worldHeightField.getWidth() + 10,
                    this.worldHeightField.getY() + 6, 0xAAAAAA);
        }
    }
}