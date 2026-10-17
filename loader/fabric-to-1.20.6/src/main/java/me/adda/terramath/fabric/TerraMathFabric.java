package me.adda.terramath.fabric;

import me.adda.terramath.TerraMath;
import me.adda.terramath.worldgen.TerraMathDensityFunction;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public class TerraMathFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Registry.register(
                BuiltInRegistries.DENSITY_FUNCTION_TYPE,
                new ResourceLocation(TerraMath.MOD_ID, "terrain"),
                TerraMathDensityFunction.CODEC.codec()
        );
        TerraMath.init();
    }
}