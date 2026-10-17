package me.adda.terramath.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.ProjectionType;
import net.minecraft.client.renderer.CoreShaders;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;

/** Version-specific immediate-mode drawing of the preview mesh. */
final class PreviewGl {
    private PreviewGl() {}

    static void drawMesh(GuiGraphics graphics, PreviewFrame frame) {
        graphics.flush();
        Minecraft mc = Minecraft.getInstance();
        Matrix4f projView = frame.projView();
        float[] verts = frame.verts();
        int floatCount = frame.floatCount();
        int vpX = frame.vpX(), vpY = frame.vpY(), vpW = frame.vpW(), vpH = frame.vpH();
        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        ProjectionType savedType = RenderSystem.getProjectionType();
        Matrix4fStack mvs = RenderSystem.getModelViewStack();

        try {
            RenderSystem.viewport(vpX, vpY, vpW, vpH);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT);
            RenderSystem.enableCull();

            RenderSystem.setProjectionMatrix(projView, ProjectionType.ORTHOGRAPHIC);
            mvs.pushMatrix();
            mvs.identity();

            RenderSystem.setShader(CoreShaders.POSITION_COLOR);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

            float[] v = verts;
            int total = floatCount; // уже в float, шаг 7
            for (int i = 0; i < total; i += 7) {
                bb.addVertex(v[i], v[i+1], v[i+2])
                        .setColor((int)v[i+3], (int)v[i+4], (int)v[i+5], (int)v[i+6]);
            }
            BufferUploader.drawWithShader(bb.buildOrThrow());

        } finally {
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            mvs.popMatrix();
            RenderSystem.setProjectionMatrix(savedProj, savedType);
            RenderSystem.viewport(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight());
        }
    }
}
