package me.adda.terramath.neoforge.platform;

import me.adda.terramath.platform.ConfigHelper;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class NeoForgeConfigHelper implements ConfigHelper {
    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}
