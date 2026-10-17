package me.adda.terramath.gui;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.PrimitiveTopology;
import net.minecraft.client.renderer.BindGroupLayouts;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Matrix4f;

public class PreviewRenderer extends PictureInPictureRenderer<PreviewRenderState> {
    private static final float DEPTH_RANGE = 1.5f;

    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
            .withLocation("pipeline/terramath_preview")
            .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
            .withCull(false)
            .build();

    private static final RenderType TYPE = RenderType.create("terramath_preview", RenderSetup.builder(PIPELINE).createRenderSetup());

    @Override
    public Class<PreviewRenderState> getRenderStateClass() {
        return PreviewRenderState.class;
    }

    @Override
    protected void renderToTexture(PreviewRenderState state, PoseStack poseStack, SubmitNodeCollector collector) {
        float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();

        poseStack.translate(0f, -(state.y1() - state.y0()) / 2f, 0f);
        poseStack.scale(state.zoom(), -state.zoom(), -DEPTH_RANGE / (guiScale * state.scale()));
        poseStack.mulPose(state.modelView());

        float[] v = state.verts();
        int floats = state.floatCount();
        collector.submitCustomGeometry(poseStack, TYPE, (pose, consumer) -> {
            Matrix4f matrix = pose.pose();
            for (int i = 0; i < floats; i += 7) {
                consumer.addVertex(matrix, v[i], v[i + 1], v[i + 2]).setColor((int) v[i + 3], (int) v[i + 4], (int) v[i + 5], (int) v[i + 6]);
            }
        });
    }

    @Override
    protected String getTextureLabel() {
        return "terramath preview";
    }
}
