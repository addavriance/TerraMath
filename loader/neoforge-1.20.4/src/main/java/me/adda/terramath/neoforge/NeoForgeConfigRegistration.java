package me.adda.terramath.neoforge;

import me.adda.terramath.config.ConfigScreenFactory;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.ConfigScreenHandler;

public class NeoForgeConfigRegistration {
    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> ConfigScreenFactory.getFactory().apply(parent)));
    }
}
