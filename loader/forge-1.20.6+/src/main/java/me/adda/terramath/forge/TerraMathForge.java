package me.adda.terramath.forge;

import com.mojang.serialization.MapCodec;
import me.adda.terramath.TerraMath;
import me.adda.terramath.worldgen.TerraMathDensityFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(TerraMath.MOD_ID)
public class TerraMathForge {

    private static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, TerraMath.MOD_ID);

    @SuppressWarnings("unused")
    private static final RegistryObject<MapCodec<? extends DensityFunction>> TERRAIN_DENSITY_FUNCTION =
            DENSITY_FUNCTION_TYPES.register("terrain", TerraMathDensityFunction.CODEC::codec);

    public TerraMathForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        DENSITY_FUNCTION_TYPES.register(modEventBus);

        ForgeConfigRegistration.register();

        TerraMath.init();
    }
}