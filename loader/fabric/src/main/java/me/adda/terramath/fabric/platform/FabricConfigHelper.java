package me.adda.terramath.fabric.platform;

import me.adda.terramath.platform.ConfigHelper;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class FabricConfigHelper implements ConfigHelper {
    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
}