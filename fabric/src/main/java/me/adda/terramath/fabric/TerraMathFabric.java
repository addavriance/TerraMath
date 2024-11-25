package me.adda.terramath.fabric;

import me.adda.terramath.TerraMath;
import net.fabricmc.api.ModInitializer;

public class TerraMathFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        TerraMath.init();
    }
}