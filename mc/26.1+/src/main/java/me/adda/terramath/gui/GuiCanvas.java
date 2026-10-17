package me.adda.terramath.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class GuiCanvas implements PreviewCanvas {
    private final GuiGraphicsExtractor graphics;

    public GuiCanvas(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int color) {
        graphics.fill(x1, y1, x2, y2, color);
    }

    @Override
    public void outline(int x, int y, int w, int h, int color) {
        graphics.outline(x, y, w, h, color);
    }

    @Override
    public void text(Font font, String text, int x, int y, int color, boolean shadow) {
        graphics.text(font, text, x, y, color, shadow);
    }

    @Override
    public void centeredText(Font font, String text, int x, int y, int color) {
        graphics.centeredText(font, text, x, y, color);
    }

    @Override
    public void mesh(PreviewFrame frame) {
        PreviewGl.drawMesh(graphics, frame);
    }
}
