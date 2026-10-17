package me.adda.terramath.gui;

import net.minecraft.client.gui.Font;

public interface PreviewCanvas {
    void fill(int x1, int y1, int x2, int y2, int color);

    void outline(int x, int y, int w, int h, int color);

    void text(Font font, String text, int x, int y, int color, boolean shadow);

    void centeredText(Font font, String text, int x, int y, int color);

    void mesh(PreviewFrame frame);
}
