package me.adda.terramath.exception;

import me.adda.terramath.math.functions.MathFunctionsRegistry;

//import java.util.regex.Matcher;
//import java.util.regex.Pattern;

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
        String function = functionMapSplitted[functionMapSplitted.length-1].replace("\"", "");

        return function.split("\\(")[0];
    }

    public static String extractVariableName(String errorMessage) {
        String prefix1 = "Unknown variable or type \"";
        String prefix2 = "Expression \"";

        int startIndex = errorMessage.indexOf(prefix1);
        String prefixUsed = prefix1;

        if (startIndex == -1) {
            startIndex = errorMessage.indexOf(prefix2);
            if (startIndex == -1) {
                return "";
            }
            prefixUsed = prefix2;
        }

        startIndex += prefixUsed.length();
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

//    public static int getErrorPosition(String errorMessage) {
//        int MAGIC_SHIFT = 58;
//
//        Pattern pattern = Pattern.compile("(?<=Column )\\d+", Pattern.MULTILINE);
//        Matcher matcher = pattern.matcher(errorMessage);
//
//        int rawIndex = 0;
//
//        if (matcher.find()) {
//            rawIndex = Integer.parseInt(matcher.group(0));
//        }
//
//        return rawIndex - MAGIC_SHIFT;
//    }

    public static String extractSyntaxError(String errorMessage) {

        int colonIndex = errorMessage.indexOf(':');
        if (colonIndex == -1 || colonIndex == errorMessage.length() - 1) {

            return errorMessage.trim();
        }

        String messagePart = errorMessage.substring(colonIndex + 1).trim();

        return messagePart;
    }
}
