package me.adda.terramath.neoforge;

import com.mojang.serialization.MapCodec;
import me.adda.terramath.TerraMath;
import me.adda.terramath.worldgen.TerraMathDensityFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(TerraMath.MOD_ID)
public class TerraMathNeoForge {

    private static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, TerraMath.MOD_ID);

    @SuppressWarnings("unused")
    private static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<? extends DensityFunction>> TERRAIN_DENSITY_FUNCTION =
            DENSITY_FUNCTION_TYPES.register("terrain", TerraMathDensityFunction.CODEC::codec);

    public TerraMathNeoForge(IEventBus modEventBus, ModContainer container) {
        DENSITY_FUNCTION_TYPES.register(modEventBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForgeConfigRegistration.register(container);
        }

        TerraMath.init();
    }
}
