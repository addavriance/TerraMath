package me.adda.terramath.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.BiFunction;

public class ConfigScreenFactory {
    public static BiFunction<Minecraft, Screen, Screen> getFactory() {
        return ConfigScreen::new;
    }

    /**
     * Creates a config screen in world creation mode
     * @param parent The parent screen to return to
     * @param isWorldCreation Flag indicating world creation mode
     * @return The configured screen
     */
    public static Screen createScreen(Screen parent, boolean isWorldCreation) {
        return new ConfigScreen(parent, isWorldCreation);
    }
}