package me.adda.terramath.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;

import static net.minecraft.client.Minecraft.DEFAULT_FONT;

public class ResetButton extends Button {
    private static final int BUTTON_SIZE = 20;
    private final TerrainSettingsSlider associatedSlider;
    private static final Component RESET_SYMBOL = Component.literal("↺").withStyle(style -> style.withFont(DEFAULT_FONT));
    private static final float SCALE = 2.5f;

    public ResetButton(int x, int y, TerrainSettingsSlider slider) {
        super(x, y, BUTTON_SIZE, BUTTON_SIZE, Component.empty(), button -> {
            slider.setValue(slider.getDefaultValue());
        }, Button.DEFAULT_NARRATION);
        this.associatedSlider = slider;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.active = Math.abs(associatedSlider.getValue() - associatedSlider.getDefaultValue()) > 0.0001;

        super.renderWidget(graphics, mouseX, mouseY, partialTick);

        int textColor = this.active ? 0xFFFFFF : 0xA0A0A0;
        float symbolWidth = Minecraft.getInstance().font.width(RESET_SYMBOL);
        float symbolHeight = Minecraft.getInstance().font.lineHeight;

        float x = this.getX() + (this.width - symbolWidth * SCALE) / 2.0f + 1;
        float y = this.getY() + (this.height - symbolHeight * SCALE) / 2.0f - 1;

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        poseStack.translate(x, y, 0);
        poseStack.scale(SCALE, SCALE, 1.0f);

        graphics.drawString(
                Minecraft.getInstance().font,
                RESET_SYMBOL,
                0,
                0,
                textColor,
                true
        );

        poseStack.popPose();
    }
}