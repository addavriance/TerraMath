package me.adda.terramath.forge;

import dev.architectury.platform.forge.EventBuses;
import me.adda.terramath.TerraMath;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TerraMath.MOD_ID)
public class TerraMathForge {
    public TerraMathForge() {
		// Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(TerraMath.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        TerraMath.init();
    }
}