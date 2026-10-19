package me.adda.terramath.neoforge;

import me.adda.terramath.gui.PreviewRenderState;
import me.adda.terramath.gui.PreviewRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterPictureInPictureRenderersEvent;

final class TerraMathNeoForgeClient {
    private TerraMathNeoForgeClient() {}

    static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPictureInPictureRenderersEvent.class,
                event -> event.register(PreviewRenderState.class, PreviewRenderer::new));
    }
}
