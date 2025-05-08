package me.adda.terramath.world;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SeedUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("TerraMath/SeedUtils");

    private static final long DEFAULT_SEED = 0L;

    private static long currentSeed = DEFAULT_SEED;

    private static boolean worldLoaded = false;

    public static long getSeed() {
        return currentSeed;
    }

    public static void setSeed(long seed) {
        LOGGER.debug("Setting world seed: {}", seed);
        currentSeed = seed;
        worldLoaded = true;
    }

    public static void resetSeed() {
        LOGGER.debug("Resetting world seed");
        currentSeed = DEFAULT_SEED;
        worldLoaded = false;
    }

    public static boolean isWorldLoaded() {
        return worldLoaded;
    }
}