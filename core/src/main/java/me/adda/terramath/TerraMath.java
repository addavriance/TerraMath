package me.adda.terramath;

import me.adda.terramath.config.ModConfig;
import me.adda.terramath.platform.PlatformHelper;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;

public class TerraMath {
	public static final String MOD_ID = "terramath";
	public static final String MOD_NAME = "TerraMath";
	public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

	public static void init() {
		PlatformHelper.getEvents().registerEvents();

		Path configDir = PlatformHelper.getConfigDir();

		ModConfig.init(configDir);

		LOGGER.info("{} initialized", MOD_NAME);
	}
}