package me.adda.terramath;

import dev.architectury.injectables.annotations.ExpectPlatform;

public interface SeedUtils {
    @ExpectPlatform
    public static long getSeed() {
        throw new AssertionError("Platform-specific implementation not found.");
    }
}