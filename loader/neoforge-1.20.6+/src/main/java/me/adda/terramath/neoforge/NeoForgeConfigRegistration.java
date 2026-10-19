package me.adda.terramath.neoforge;

import me.adda.terramath.config.ConfigScreenFactory;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public class NeoForgeConfigRegistration {
    public static void register(ModContainer container) {
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (modContainer, parent) -> ConfigScreenFactory.getFactory().apply(parent)
        );
    }
}
