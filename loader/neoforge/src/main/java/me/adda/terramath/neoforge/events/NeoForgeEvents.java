package me.adda.terramath.neoforge.events;

import me.adda.terramath.command.TerraMathCommand;
import me.adda.terramath.events.PlatformEvents;
import me.adda.terramath.events.TerraMathEvents;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

public class NeoForgeEvents implements PlatformEvents {

    @Override
    public void registerEvents() {
        NeoForge.EVENT_BUS.register(NeoForgeEvents.class);
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            TerraMathEvents.onLevelLoad(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            TerraMathEvents.onLevelUnload(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        TerraMathCommand.register(event.getDispatcher());
    }
}
