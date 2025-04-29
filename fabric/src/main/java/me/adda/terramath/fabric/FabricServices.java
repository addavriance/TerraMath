package me.adda.terramath.fabric;

import me.adda.terramath.platform.PlatformServices;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class FabricServices implements PlatformServices {
    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }
}