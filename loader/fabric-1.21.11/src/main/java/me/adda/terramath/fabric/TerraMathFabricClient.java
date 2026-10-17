package me.adda.terramath.fabric;

import me.adda.terramath.gui.PreviewRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry;

public class TerraMathFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SpecialGuiElementRegistry.register(context -> new PreviewRenderer(context.vertexConsumers()));
    }
}
