package me.adda.terramath.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;

public class SeedUtilsImpl {
    public static long getSeed() {
        Minecraft client = Minecraft.getInstance();
        if (client.level != null && client.getSingleplayerServer() != null) {
            ServerLevel serverLevel = client.getSingleplayerServer().overworld();
            return serverLevel.getSeed();
        }
        return 0L;
    }
}