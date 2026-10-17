package me.adda.terramath.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;

/** Version-specific immediate-mode drawing of the preview mesh. */
final class PreviewGl {
    private PreviewGl() {}

    static void drawMesh(Matrix4f projView, float[] verts, int floatCount, int vpX, int vpY, int vpW, int vpH) {
        Minecraft mc = Minecraft.getInstance();
        Matrix4f savedProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4fStack mvs = RenderSystem.getModelViewStack();

        try {
            RenderSystem.viewport(vpX, vpY, vpW, vpH);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            RenderSystem.enableCull();

            RenderSystem.setProjectionMatrix(projView, RenderSystem.getVertexSorting());
            mvs.pushMatrix();
            mvs.identity();
            RenderSystem.applyModelViewMatrix();

            RenderSystem.setShader(GameRenderer::getPositionColorShader);
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
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(savedProj, RenderSystem.getVertexSorting());
            RenderSystem.viewport(0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight());
        }
    }
}
