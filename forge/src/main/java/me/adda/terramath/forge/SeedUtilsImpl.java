package me.adda.terramath.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class SeedUtilsImpl {
    public static long getSeed() {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            ServerLevel serverLevel = server.overworld();
            return serverLevel.getSeed();
        }
        return 0L;
    }
}