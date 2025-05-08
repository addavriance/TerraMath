package me.adda.terramath.forge.platform;

import me.adda.terramath.platform.ConfigHelper;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ForgeConfigHelper implements ConfigHelper {
    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }
}