package me.adda.terramath.fabric;

import me.adda.terramath.gui.PreviewRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.PictureInPictureRendererRegistry;

public class TerraMathFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PictureInPictureRendererRegistry.register(context -> new PreviewRenderer());
    }
}
