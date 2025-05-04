package me.adda.terramath.math.formula;

import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.math.functions.MathFunctionsRegistry;
import me.adda.terramath.math.parser.FactorialParser;
import me.adda.terramath.math.parser.PowerParser;

import java.util.Locale;

public class FormulaFormatter {
    public static String convertToJavaExpression(String formula) {
        String javaExpr = formula;

        javaExpr = wrapWithNoise(javaExpr);

        for (String funcName : MathFunctionsRegistry.getSortedFunctionNames()) {
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

    private static String replacePowerOperator(String expression) {
        PowerParser parser = new PowerParser(expression);

        return parser.parse();
    }

    private static String handleFactorials(String expression) {
        FactorialParser parser = new FactorialParser(expression);

        return parser.parse();
    }

    private static String wrapWithNoise(String expression) {
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

}
