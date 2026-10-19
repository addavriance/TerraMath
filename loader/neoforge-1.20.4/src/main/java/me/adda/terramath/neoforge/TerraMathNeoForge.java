package me.adda.terramath.neoforge;

import com.mojang.serialization.Codec;
import me.adda.terramath.TerraMath;
import me.adda.terramath.worldgen.TerraMathDensityFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

@Mod(TerraMath.MOD_ID)
public class TerraMathNeoForge {

    private static final DeferredRegister<Codec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, TerraMath.MOD_ID);

    @SuppressWarnings("unused")
    private static final DeferredHolder<Codec<? extends DensityFunction>, Codec<? extends DensityFunction>> TERRAIN_DENSITY_FUNCTION =
            DENSITY_FUNCTION_TYPES.register("terrain", TerraMathDensityFunction.CODEC::codec);

    public TerraMathNeoForge(IEventBus modEventBus) {
        DENSITY_FUNCTION_TYPES.register(modEventBus);

        NeoForgeConfigRegistration.register();

        TerraMath.init();
    }
}
