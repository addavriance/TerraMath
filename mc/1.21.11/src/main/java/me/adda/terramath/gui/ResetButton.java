package me.adda.terramath.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;

public class ResetButton extends Button.Plain {
    private static final int BUTTON_SIZE = 20;
    private final TerrainSettingsSlider associatedSlider;
    private static final Component RESET_SYMBOL = Component.literal("↺").withStyle(style -> style.withFont(FontDescription.DEFAULT));
    private static final float SCALE = 2.5f;

    public ResetButton(int x, int y, TerrainSettingsSlider slider) {
        super(x, y, BUTTON_SIZE, BUTTON_SIZE, Component.empty(), button -> {
            slider.setRealValue(slider.getDefaultValue());
        }, Button.DEFAULT_NARRATION);
        this.associatedSlider = slider;
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.active = Math.abs(associatedSlider.getValue() - associatedSlider.getDefaultValue()) > 0.0001;

        int textColor = this.active ? 0xFFFFFFFF : 0xFFA0A0A0;
        float symbolWidth = Minecraft.getInstance().font.width(RESET_SYMBOL);
        float symbolHeight = Minecraft.getInstance().font.lineHeight;

        float x = this.getX() + (this.width - symbolWidth * SCALE) / 2.0f + 1;
        float y = this.getY() + (this.height - symbolHeight * SCALE) / 2.0f - 1;

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(SCALE, SCALE);

        graphics.drawString(Minecraft.getInstance().font, RESET_SYMBOL, 0, 0, textColor, true);

        graphics.pose().popMatrix();
    }
}
