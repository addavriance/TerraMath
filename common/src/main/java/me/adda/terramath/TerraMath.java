package me.adda.terramath;

import me.adda.terramath.events.TerraMathEvents;
import me.adda.terramath.platform.PlatformHelper;

public class TerraMath {
	public static final String MOD_ID = "terramath";
	public static final String MOD_NAME = "TerraMath";

	public static void init() {
		PlatformHelper.getEvents().registerEvents();
	}
}