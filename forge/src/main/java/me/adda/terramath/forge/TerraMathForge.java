package me.adda.terramath.forge;

import me.adda.terramath.TerraMath;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TerraMath.MOD_ID)
public class TerraMathForge {
    public TerraMathForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        registerForgeEvents(modEventBus);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> this::registerClientOnlyConfig);

        TerraMath.init();
    }

    private void registerForgeEvents(IEventBus eventBus) {
        // MyBlocks.BLOCKS.register(eventBus);
        // MyItems.ITEMS.register(eventBus);

        // eventBus.register(SomeEventHandler.class);
    }

    private void registerClientOnlyConfig() {
        ForgeConfigRegistration.register();
    }
}