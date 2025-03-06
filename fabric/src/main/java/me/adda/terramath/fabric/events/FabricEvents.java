package me.adda.terramath.fabric.events;

import dev.architectury.event.events.common.LifecycleEvent;
import me.adda.terramath.events.PlatformEvents;
import me.adda.terramath.events.TerraMathEvents;

public class FabricEvents implements PlatformEvents {
    @Override
    public void registerEvents() {
        LifecycleEvent.SERVER_LEVEL_LOAD.register(TerraMathEvents::onLevelLoad);
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(TerraMathEvents::onLevelUnload);
    }
}