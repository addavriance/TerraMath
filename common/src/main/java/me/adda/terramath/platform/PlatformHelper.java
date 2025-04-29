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

    public static Path getConfigDirectory() {
        return ServiceLoader.load(PlatformServices.class)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Failed to load platform services"))
                .getConfigDirectory();
    }
}