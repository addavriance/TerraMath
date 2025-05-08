package me.adda.terramath.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("TerraMath/NotificationManager");

    public static void showFormulaError(String formula) {
        LOGGER.warn("Invalid formula detected and reset: {}", formula);

        try {
            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.level != null) {
                SystemToast.addOrUpdate(
                        minecraft.getToasts(),
                        SystemToast.SystemToastIds.WORLD_ACCESS_FAILURE,
                        Component.translatable("terramath.formula.error.invalid_reset.title"),
                        Component.translatable("terramath.formula.error.invalid_reset.description")
                );
            }
        } catch (Exception ignored) {}
    }
}
