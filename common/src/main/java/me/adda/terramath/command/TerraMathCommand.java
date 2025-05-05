package me.adda.terramath.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.adda.terramath.api.TerrainFormulaManager;
import me.adda.terramath.api.TerrainSettingsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

public class TerraMathCommand {
    public static final String TRANSLATION_PREFIX = "terramath.command.";

    public static final String COMMAND_FORMUlA_INFO = TRANSLATION_PREFIX + "formula";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command_terramath = Commands.literal("terramath")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("formula")
                        .executes(TerraMathCommand::executeFormulaCommand)
                ).then(Commands.literal("settings")
                        .executes(TerraMathCommand::executeSettingsCommand)
                ).then(Commands.literal("info")
                        .executes(TerraMathCommand::executeInfoCommand));

        LiteralArgumentBuilder<CommandSourceStack> command_formula = Commands.literal("formula")
                .requires(source -> source.hasPermission(2))
                .executes(TerraMathCommand::executeFormulaCommand);

        dispatcher.register(command_terramath);

        dispatcher.register(command_formula);
    }

    private static int executeInfoCommand(CommandContext<CommandSourceStack> context) {
        executeFormulaCommand(context);
        executeSettingsCommand(context);

        return 1;
    }

    private static int executeFormulaCommand(CommandContext<CommandSourceStack> context) {
        String formula = TerrainFormulaManager.getInstance().getFormula();

        MutableComponent message = Component.translatable(COMMAND_FORMUlA_INFO).append(Component.literal("["))
                .append(
                        Component.literal(formula)
                                .withStyle(style -> style
                                        .withColor(ChatFormatting.GREEN)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, formula))
                                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.copy.click")))
                                )
                )
                .append(Component.literal("]"));

        context.getSource().sendSuccess(() -> message, false);

        return 1;
    }

    private static int executeSettingsCommand(CommandContext<CommandSourceStack> context) {
        TerrainSettingsManager settings = TerrainSettingsManager.getInstance();

        MutableComponent coordinateScale = Component.translatable("terramath.config.coordinate_scale")
                .append(": ")
                .append(Component.literal(String.format("%.2f", settings.getCoordinateScale()))
                        .withStyle(ChatFormatting.GREEN));

        MutableComponent densityMode = Component.translatable("terramath.config.density_mode")
                .append(": ")
                .append(Component.literal(String.valueOf(settings.isUseDensityMode()))
                        .withStyle(settings.isUseDensityMode() ? ChatFormatting.GREEN : ChatFormatting.RED));

        MutableComponent baseHeight = Component.translatable("terramath.config.base_height")
                .append(": ")
                .append(Component.literal(String.format("%.2f", settings.getBaseHeight()))
                        .withStyle(ChatFormatting.GREEN));

        MutableComponent heightVariation = Component.translatable("terramath.config.height_variation")
                .append(": ")
                .append(Component.literal(String.format("%.2f", settings.getHeightVariation()))
                        .withStyle(ChatFormatting.GREEN));

        MutableComponent smoothing = Component.translatable("terramath.config.smoothing")
                .append(": ")
                .append(Component.literal(String.format("%.2f", settings.getSmoothingFactor()))
                        .withStyle(ChatFormatting.GREEN));

        MutableComponent noiseType = Component.translatable("terramath.config.noise_type")
                .append(": ")
                .append(Component.translatable("terramath.config.noise_type." + settings.getNoiseType().name().toLowerCase())
                        .withStyle(ChatFormatting.GREEN));

        MutableComponent noiseScaleX = Component.translatable("terramath.config.noise_scale_x")
                .append(": ")
                .append(Component.literal(String.format("%.2f", settings.getNoiseScaleX()))
                        .withStyle(ChatFormatting.GREEN));

        MutableComponent noiseScaleY = Component.translatable("terramath.config.noise_scale_y")
                .append(": ")
                .append(Component.literal(String.format("%.2f", settings.getNoiseScaleY()))
                        .withStyle(ChatFormatting.GREEN));

        MutableComponent noiseScaleZ = Component.translatable("terramath.config.noise_scale_z")
                .append(": ")
                .append(Component.literal(String.format("%.2f", settings.getNoiseScaleZ()))
                        .withStyle(ChatFormatting.GREEN));

        MutableComponent noiseHeightScale = Component.translatable("terramath.config.noise_height_scale")
                .append(": ")
                .append(Component.literal(String.format("%.2f", settings.getNoiseHeightScale()))
                        .withStyle(ChatFormatting.GREEN));


        context.getSource().sendSuccess(() -> Component.literal(""), false);

        context.getSource().sendSuccess(() -> coordinateScale, false);

        context.getSource().sendSuccess(() -> densityMode, false);

        if (settings.isUseDensityMode()) {
            context.getSource().sendSuccess(() -> baseHeight, false);
            context.getSource().sendSuccess(() -> heightVariation, false);
            context.getSource().sendSuccess(() -> smoothing, false);
        }

        context.getSource().sendSuccess(() -> Component.literal(""), false);

        context.getSource().sendSuccess(() -> noiseType, false);
        context.getSource().sendSuccess(() -> noiseScaleX, false);
        context.getSource().sendSuccess(() -> noiseScaleY, false);
        context.getSource().sendSuccess(() -> noiseScaleZ, false);
        context.getSource().sendSuccess(() -> noiseHeightScale, false);

        return 1;
    }
}