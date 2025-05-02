package me.adda.terramath.forge;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory;
import net.minecraftforge.fml.ModLoadingContext;

import static me.adda.terramath.config.ConfigScreenFactory.getFactory;

public class ForgeConfigRegistration {
    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(getFactory())
        );
    }
}