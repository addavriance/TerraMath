package me.adda.terramath.exception;

import me.adda.terramath.math.functions.MathFunctionsRegistry;

public class ExceptionUtils {
    public static String extractFunctionName(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return "";
        }

        for (String functionName : MathFunctionsRegistry.getSortedFunctionNames()) {
            String implementation = MathFunctionsRegistry.getFunctionImplementation(functionName);

            if (errorMessage.contains(implementation)) {
                return functionName;
            }
        }

        // Super simple method (fallback)
        String functionMap = errorMessage.split("candidates are: ")[1];
        String[] functionMapSplitted = functionMap.split("\\.");
        String function = functionMapSplitted[functionMapSplitted.length-1].replace("\"\\", "");

        return function.split("\\(")[0];
    }

    public static String extractVariableName(String errorMessage) {
        String prefix = "Unknown variable or type \"";
        int startIndex = errorMessage.indexOf(prefix);
        if (startIndex == -1) {
            return "";
        }
        startIndex += prefix.length();
        int endIndex = errorMessage.indexOf("\"", startIndex);
        if (endIndex == -1) {
            return "";
        }
        return errorMessage.substring(startIndex, endIndex);
    }

    public static Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}
