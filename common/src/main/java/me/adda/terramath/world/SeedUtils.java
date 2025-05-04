package me.adda.terramath.world;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class SeedUtils {

    public static long getSeed() {
        Minecraft minecraft = Minecraft.getInstance();
        IntegratedServer integratedServer = minecraft.getSingleplayerServer();

        if (integratedServer != null) {
            ServerLevel overworld = integratedServer.getLevel(Level.OVERWORLD);
            if (overworld != null) {
                return overworld.getSeed();
            }
        }

        return 0;
    }
}