package me.adda.terramath.config;

import net.minecraft.client.gui.screens.Screen;

import java.util.function.Function;

public class ConfigScreenFactory {
    public static Function<Screen, Screen> getFactory() {
        return ConfigScreen::new;
    }
}