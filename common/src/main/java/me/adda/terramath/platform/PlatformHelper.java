package me.adda.terramath.platform;

import me.adda.terramath.events.PlatformEvents;

import java.nio.file.Path;
import java.util.ServiceLoader;

public class PlatformHelper {
    public static PlatformEvents getEvents() {
        return ServiceLoader.load(PlatformEvents.class)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Failed to load platform events"));
    }

    public static ConfigHelper getConfigHelper() {
        return ServiceLoader.load(ConfigHelper.class)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Failed to load config helper"));
    }

    public static Path getConfigDir() {
        return getConfigHelper().getConfigDir();
    }
}