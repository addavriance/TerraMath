package me.adda.terramath;

import me.adda.terramath.events.TerraMathEvents;

public class TerraMath {
	public static final String MOD_ID = "terramath";
	public static final String MOD_NAME = "TerraMath";

	public static void init() {
		TerraMathEvents.init();
	}
}