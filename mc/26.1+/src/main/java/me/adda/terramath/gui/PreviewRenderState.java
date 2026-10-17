package me.adda.terramath.gui;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.joml.Matrix4f;

public record PreviewRenderState(int x0, int y0, int x1, int y1, float scale,
                                 ScreenRectangle scissorArea, ScreenRectangle bounds,
                                 float[] verts, int floatCount, Matrix4f modelView, float zoom)
        implements PictureInPictureRenderState {
}
