package me.adda.terramath.gui;

import me.adda.terramath.mixin.GuiGraphicsAccessor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;

final class PreviewGl {
    private PreviewGl() {}

    static void drawMesh(GuiGraphics graphics, PreviewFrame frame) {
        ScreenRectangle scissor = ((GuiGraphicsAccessor) graphics).terramath$scissorStack().peek();
        int x1 = frame.x() + frame.w();
        int y1 = frame.y() + frame.h();
        ((GuiGraphicsAccessor) graphics).terramath$guiRenderState().submitPicturesInPictureState(new PreviewRenderState(
                frame.x(), frame.y(), x1, y1, 1f, scissor,
                PictureInPictureRenderState.getBounds(frame.x(), frame.y(), x1, y1, scissor),
                frame.verts(), frame.floatCount(), frame.modelView(), frame.zoom()));
    }
}
