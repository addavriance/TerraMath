package me.adda.terramath.fabric.events;

import me.adda.terramath.command.TerraMathCommand;
import me.adda.terramath.events.PlatformEvents;
import me.adda.terramath.events.TerraMathEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;

public class FabricEvents implements PlatformEvents {
    @Override
    public void registerEvents() {
        ServerLevelEvents.LOAD.register((server, level) -> {
            TerraMathEvents.onLevelLoad(level);
        });

        ServerLevelEvents.UNLOAD.register((server, level) -> {
            TerraMathEvents.onLevelUnload(level);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TerraMathCommand.register(dispatcher);
        });
    }
}
