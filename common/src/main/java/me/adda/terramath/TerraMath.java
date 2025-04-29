package me.adda.terramath;

import me.adda.terramath.config.ModConfig;
import me.adda.terramath.platform.PlatformHelper;

import java.nio.file.Path;

public class TerraMath {
	public static final String MOD_ID = "terramath";
	public static final String MOD_NAME = "TerraMath";

	public static void init() {
		PlatformHelper.getEvents().registerEvents();


		Path configDir = PlatformHelper.getConfigDirectory();
		ModConfig.init(configDir);
	}
}