package me.adda.terramath.gui;

import me.adda.terramath.mixin.GuiGraphicsExtractorAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;

final class PreviewGl {
    private PreviewGl() {}

    static void drawMesh(GuiGraphicsExtractor graphics, PreviewFrame frame) {
        ScreenRectangle scissor = null;
        int x1 = frame.x() + frame.w();
        int y1 = frame.y() + frame.h();
        ((GuiGraphicsExtractorAccessor) graphics).terramath$guiRenderState().addPicturesInPictureState(new PreviewRenderState(
                frame.x(), frame.y(), x1, y1, 1f, scissor,
                PictureInPictureRenderState.getBounds(frame.x(), frame.y(), x1, y1, scissor),
                frame.verts(), frame.floatCount(), frame.modelView(), frame.zoom()));
    }
}
