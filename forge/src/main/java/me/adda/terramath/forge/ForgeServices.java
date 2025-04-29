package me.adda.terramath.forge;

import me.adda.terramath.platform.PlatformServices;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ForgeServices implements PlatformServices {
    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }
}