package me.adda.terramath.forge;

import me.adda.terramath.config.ConfigScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory;
import net.minecraftforge.fml.ModLoadingContext;

import java.util.function.Function;

public class ForgeConfigRegistration {
    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((Function<Screen, Screen>) ConfigScreen::new)
        );
    }
}