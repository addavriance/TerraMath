package me.adda.terramath.forge;

import me.adda.terramath.gui.PreviewRenderer;
import net.minecraftforge.client.event.RegisterPictureInPictureRendererEvent;

final class TerraMathForgeClient {
    private TerraMathForgeClient() {}

    static void register() {
        RegisterPictureInPictureRendererEvent.BUS.addListener(event -> event.register(new PreviewRenderer()));
    }
}
