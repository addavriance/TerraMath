package me.adda.terramath.forge;

import me.adda.terramath.config.ConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory;
import net.minecraftforge.fml.ModLoadingContext;

public class ForgeConfigRegistration {
    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(ConfigScreen::new)
        );
    }
}