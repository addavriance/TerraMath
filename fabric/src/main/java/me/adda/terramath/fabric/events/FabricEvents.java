package me.adda.terramath.fabric.events;

import me.adda.terramath.events.PlatformEvents;
import me.adda.terramath.events.TerraMathEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

public class FabricEvents implements PlatformEvents {
    @Override
    public void registerEvents() {
        ServerWorldEvents.LOAD.register((server, world) -> {
            TerraMathEvents.onLevelLoad(world);
        });

        ServerWorldEvents.UNLOAD.register((server, world) -> {
            TerraMathEvents.onLevelUnload(world);
        });
    }
}