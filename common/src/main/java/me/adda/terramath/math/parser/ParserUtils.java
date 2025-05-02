package me.adda.terramath.math.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParserUtils {
    public static boolean isFunction(String expression, int pos) {
        if (pos == 0) {
            return false;
        }

        Pattern pattern = Pattern.compile("([a-zA-Z_.][a-zA-Z0-9_.]*)$");
        Matcher matcher = pattern.matcher(expression.substring(0, pos));

        return matcher.find();
    }

    public static int findFunctionStart(String expression, int bracketPos) {
        Pattern pattern = Pattern.compile("([a-zA-Z_.][a-zA-Z0-9_.]*)$");
        Matcher matcher = pattern.matcher(expression.substring(0, bracketPos));

        if (matcher.find()) {
            return bracketPos - matcher.group(1).length();
        }

        return bracketPos;
    }
}
