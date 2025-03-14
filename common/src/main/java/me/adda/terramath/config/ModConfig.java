package me.adda.terramath.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.function.Consumer;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig INSTANCE;
    private static File configFile;

    public String baseFormula = "";
    public boolean useDefaultFormula = true;

    public boolean customWorldHeight = false;
    public int worldHeight = 384;
    public boolean symmetricHeight = true;

    private static Consumer<ModConfig> saveCallback = null;

    public static void init(Path configDir) {
        configFile = configDir.resolve("terramath.json").toFile();

        if (INSTANCE == null) {
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    INSTANCE = GSON.fromJson(reader, ModConfig.class);
                } catch (IOException e) {
                    INSTANCE = new ModConfig();
                    save();
                }
            } else {
                INSTANCE = new ModConfig();
                save();
            }
        }
    }

    public static ModConfig get() {
        if (INSTANCE == null) {
            INSTANCE = new ModConfig();
            save();
        }
        return INSTANCE;
    }

    public static void save() {
        try {
            if (!configFile.getParentFile().exists()) {
                Files.createDirectories(configFile.getParentFile().toPath());
            }

            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(INSTANCE, writer);
            }

            if (saveCallback != null) {
                saveCallback.accept(INSTANCE);
            }
        } catch (IOException e) {
            // Error, where did you go?
        }
    }

    public static String generateRandomFormula() {
        Random random = new Random();
        String[] operators = {"+", "-", "*", "/"};
        String[] functions = {"sin", "cos", "abs", "sqrt"};
        StringBuilder formula = new StringBuilder();

        int terms = random.nextInt(3) + 2;
        boolean useFunctions = random.nextBoolean();

        for (int i = 0; i < terms; i++) {
            int coefficient = random.nextInt(10) + 1;
            int power = random.nextInt(3);

            if (i > 0) {
                formula.append(" ").append(operators[random.nextInt(operators.length)]).append(" ");
            }

            if (useFunctions && random.nextInt(3) == 0) {
                String function = functions[random.nextInt(functions.length)];
                formula.append(coefficient).append("*").append(function).append("(x");

                if (random.nextBoolean()) {
                    formula.append("*").append(random.nextInt(5) + 1);
                }

                formula.append(")");
            } else {
                formula.append(coefficient);

                if (power > 0) {
                    formula.append("*x");
                    if (power > 1) {
                        formula.append("^").append(power);
                    }
                }
            }
        }

        return formula.toString();
    }

    public static void setSaveCallback(Consumer<ModConfig> callback) {
        saveCallback = callback;
    }
}