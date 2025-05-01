package me.adda.terramath;

import me.adda.terramath.config.ModConfig;
import me.adda.terramath.platform.PlatformHelper;
import net.minecraft.client.Minecraft;

import java.io.File;

public class TerraMath {
	public static final String MOD_ID = "terramath";
	public static final String MOD_NAME = "TerraMath";

	public static void init() {
		PlatformHelper.getEvents().registerEvents();

		File minecraftDir = Minecraft.getInstance().gameDirectory;

		File configDir = new File(minecraftDir, "config");
		ModConfig.init(configDir.toPath());
	}
}