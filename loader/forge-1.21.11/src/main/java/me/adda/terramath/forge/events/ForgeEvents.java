package me.adda.terramath.forge.events;

import me.adda.terramath.command.TerraMathCommand;
import me.adda.terramath.events.PlatformEvents;
import me.adda.terramath.events.TerraMathEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.level.LevelEvent;

public class ForgeEvents implements PlatformEvents {

    @Override
    public void registerEvents() {
        LevelEvent.Load.BUS.addListener(ForgeEvents::onLevelLoad);
        LevelEvent.Unload.BUS.addListener(ForgeEvents::onLevelUnload);
        RegisterCommandsEvent.BUS.addListener(ForgeEvents::onCommandsRegister);
    }

    private static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            TerraMathEvents.onLevelLoad(serverLevel);
        }
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            TerraMathEvents.onLevelUnload(serverLevel);
        }
    }

    private static void onCommandsRegister(RegisterCommandsEvent event) {
        TerraMathCommand.register(event.getDispatcher());
    }
}
