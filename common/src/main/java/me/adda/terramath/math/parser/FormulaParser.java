package me.adda.terramath.math.parser;

import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.exception.FormulaException;
import me.adda.terramath.math.functions.CompositeNoise;
import me.adda.terramath.math.formula.FormulaValidator;
import me.adda.terramath.math.functions.MathFunctionsRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import org.codehaus.janino.ExpressionEvaluator;

import java.util.Locale;


public class FormulaParser extends FormulaValidator {
    static Minecraft minecraft = Minecraft.getInstance();
    static IntegratedServer integratedServer;

    public static class CompiledFormula {
        private final String originalExpression;
        private final ExpressionEvaluator evaluator;
        private final CompositeNoise noise;

        private CompiledFormula(String expression, ExpressionEvaluator evaluator, CompositeNoise noise) {
            this.originalExpression = expression;
            this.evaluator = evaluator;
            this.noise = noise;
        }

        public double evaluate(double x, double y, double z) {
            try {
                return (double) evaluator.evaluate(new Object[]{x, y, z, this.noise});
            } catch (Exception e) {
                throw new FormulaException(ERROR_INVALID_SYNTAX, e.getMessage());
            }
        }

        public String getOriginalExpression() {
            return originalExpression;
        }
    }


    public static CompiledFormula parse(String formula) {
        if (!validateFormula(formula, true).isValid()) {
            formula = "0";
        }

        long seed = getSeed();

        String javaExpression = convertToJavaExpression(formula);

        try {
            ExpressionEvaluator evaluator = new ExpressionEvaluator();

            evaluator.setParameters(
                    new String[]{"x", "y", "z", "noise"},
                    new Class[]{double.class, double.class, double.class, CompositeNoise.class}
            );

            evaluator.setParentClassLoader(FormulaParser.class.getClassLoader());
            evaluator.setExpressionType(double.class);

            String fullExpression = String.format(
                    "import me.adda.terramath.math.functions.MathExtensions; " +
                    "(%s)",
                    javaExpression
            );

            CompositeNoise noise = new CompositeNoise(seed);

            evaluator.cook(fullExpression);
            return new CompiledFormula(formula, evaluator, noise);
        } catch (Exception e) {
            Throwable cause = getRootCause(e);
            String message = cause.getMessage();

            if (cause instanceof IllegalArgumentException ||
                    cause.getMessage() != null && message.contains("No applicable constructor")) {
                throw new FormulaException(ERROR_INVALID_ARGUMENTS);
            } else if (cause.getMessage() != null && (message.contains("Unknown variable") || message.contains("is not an rvalue"))) {
                throw new FormulaException(ERROR_UNKNOWN_VARIABLE, extractFunctionName(message));
            } else {
                throw new FormulaException(ERROR_INVALID_SYNTAX, message);
            }
        }
    }

    private static String extractFunctionName(String errorMessage) {
        if (errorMessage != null) {
            String[] parts = errorMessage.split("\"");
            if (parts.length >= 2) {
                return parts[1];
            }
        }
        return "";
    }

    private static Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    private static String convertToJavaExpression(String formula) {
        String javaExpr = formula;

        javaExpr = wrapWithNoise(javaExpr);

        for (String funcName : MathFunctionsRegistry.getFunctionNames()) {
            javaExpr = javaExpr.replaceAll(
                    "\\b" + funcName + "\\b",
                    MathFunctionsRegistry.getFunctionImplementation(funcName)
            );
        }

        javaExpr = replacePowerOperator(javaExpr);

        if (javaExpr.contains("!")) {
            javaExpr = handleFactorials(javaExpr);
        }


        return javaExpr;
    }

    public static String replacePowerOperator(String expression) {
        PowerParser parser = new PowerParser(expression);

        return parser.parse();
    }

    private static String handleFactorials(String expression) {
        FactorialParser parser = new FactorialParser(expression);

        return parser.parse();
    }

    public static String wrapWithNoise(String expression) {
        TerrainSettingsManager settingsManager = TerrainSettingsManager.getInstance();

        String noiseType = settingsManager.getNoiseType().name().toLowerCase();

        double scaleX = settingsManager.getNoiseScaleX();
        double scaleY = settingsManager.getNoiseScaleY();
        double scaleZ = settingsManager.getNoiseScaleZ();
        double heightScale = settingsManager.getNoiseHeightScale();

        if ("none".equals(noiseType)) {
            return expression;
        }

        String noiseCall;

        if (noiseType.equals("simplex")) {
            noiseCall = String.format(Locale.US, "%s(x/%.3f, z/%.3f)", noiseType, scaleX, scaleZ);
        } else {
            noiseCall = String.format(Locale.US, "%s(x/%.3f, y/%.3f, z/%.3f)", noiseType, scaleX, scaleY, scaleZ);
        }

        String result = String.format(Locale.US, "(%s) + %s*%.3f", expression, noiseCall, heightScale);

        return result;
    }

    private static long getSeed() {
        if (integratedServer == null) {
            integratedServer = minecraft.getSingleplayerServer();
            if (integratedServer == null) {
                return 0;
            }
        }

        return integratedServer.overworld().getSeed();
    }
}