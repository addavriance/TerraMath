package me.adda.terramath.math.formula;

import me.adda.terramath.math.functions.MathFunctionsRegistry;

/**
 * Converts Desmos LaTeX expressions to TerraMath formula syntax.
 *
 * Supported transformations:
 *   - Function definitions: f\left(x,\ y\right)= → stripped
 *   - \frac{a}{b}          → (a)/(b)
 *   - \sqrt{x}             → sqrt(x)
 *   - \sqrt[n]{x}          → root(x,n)
 *   - ^{expr}              → ^(expr)
 *   - \left( \right)       → ( )
 *   - \left| \right|       → abs(...)
 *   - \sin \cos etc.       → sin cos etc.
 *   - \pi \phi             → π φ
 *   - \cdot \times         → *
 *   - Implicit multiplication: 2x → 2*x, )( → )*(
 */
public class LatexConverter {

    private static final char ZY_SWAP_PLACEHOLDER = '\uE000'; // Unicode private-use char

    /**
     * Converts a Desmos LaTeX expression to TerraMath formula syntax.
     *
     * @param latex  the LaTeX string from Desmos clipboard
     * @param swapZY if true, swaps y and z variables (math convention: z=height → Minecraft y=height)
     * @return the converted formula string
     */
    public static String convert(String latex, boolean swapZY) {
        if (latex == null || latex.trim().isEmpty()) return latex;

        String result = latex.trim();

        // 1. Strip function definition prefix: f\left(x,\ y\right)= or f(x,y,z)=
        result = stripFunctionPrefix(result);

        // 2. Handle \frac{a}{b} → (a)/(b) — recursive, before other substitutions
        result = convertFrac(result);

        // 3. Handle \sqrt[n]{x} → root(x,n) — before \sqrt{x}
        result = convertNthRoot(result);

        // 4. Handle \sqrt{x} → sqrt(x)
        result = convertSqrt(result);

        // 5. Handle ^{expr} → ^(expr)
        result = convertPowerBraces(result);

        // 6. Replace \left| ... \right| → abs(...) — before \left( \right)
        result = convertAbsValue(result);

        // 7. Replace bracket commands
        result = result.replace("\\left(", "(");
        result = result.replace("\\right)", ")");
        result = result.replace("\\left[", "(");
        result = result.replace("\\right]", ")");
        result = result.replace("\\left\\{", "(");
        result = result.replace("\\right\\}", ")");

        // 8. LaTeX function names → TerraMath names
        result = convertFunctionNames(result);

        // 9. Constants and operators
        result = result.replace("\\pi", "π");
        result = result.replace("\\phi", "φ");
        result = result.replace("\\cdot", "*");
        result = result.replace("\\times", "*");
        result = result.replace("\\div", "/");

        // 10. LaTeX spacing commands
        result = result.replace("\\ ", "");
        result = result.replace("\\,", "");
        result = result.replace("\\!", "");

        // 11. Remove leftover braces (after all recursive replacements)
        result = result.replace("{", "").replace("}", "");

        // 12. Add implicit multiplication (e.g. 2x → 2*x)
        result = addImplicitMultiplication(result);

        // 13. Remove all remaining whitespace
        result = result.replaceAll("\\s+", "");

        // 14. Swap y ↔ z if requested (math uses z for height, Minecraft uses y)
        if (swapZY) {
            result = swapZAndY(result);
        }

        return result;
    }

    /**
     * Returns true if the input looks like LaTeX (contains a backslash).
     */
    public static boolean isLatex(String input) {
        return input != null && input.contains("\\");
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static String stripFunctionPrefix(String latex) {
        // f\left(x,\ y\right)= pattern (Desmos function notation)
        String stripped = latex.replaceAll(
                "^[a-zA-Z]?\\\\left\\(.*?\\\\right\\)\\s*=\\s*", "");
        if (!stripped.equals(latex)) return stripped;

        // f(x,y)= or f(x,y,z)= pattern
        stripped = latex.replaceAll("^[a-zA-Z]?\\([^)]*\\)\\s*=\\s*", "");
        if (!stripped.equals(latex)) return stripped;

        // Single-variable assignment like "z=" or "y=" at the very start
        stripped = latex.replaceAll("^[a-zA-Z]\\s*=\\s*", "");
        return stripped;
    }

    private static String convertFrac(String input) {
        String result = input;
        int idx;
        while ((idx = result.indexOf("\\frac")) != -1) {
            int braceStart = idx + 5;
            int[] numBounds = extractBraces(result, braceStart);
            if (numBounds == null) break;
            String numerator = convertFrac(result.substring(numBounds[0] + 1, numBounds[1]));

            int[] denBounds = extractBraces(result, numBounds[1] + 1);
            if (denBounds == null) break;
            String denominator = convertFrac(result.substring(denBounds[0] + 1, denBounds[1]));

            result = result.substring(0, idx)
                    + "(" + numerator + ")/(" + denominator + ")"
                    + result.substring(denBounds[1] + 1);
        }
        return result;
    }

    private static String convertNthRoot(String input) {
        String result = input;
        int idx;
        while ((idx = result.indexOf("\\sqrt[")) != -1) {
            int closeBracket = result.indexOf(']', idx + 6);
            if (closeBracket == -1) break;
            String n = result.substring(idx + 6, closeBracket);

            int[] bounds = extractBraces(result, closeBracket + 1);
            if (bounds == null) break;
            String x = convertNthRoot(result.substring(bounds[0] + 1, bounds[1]));

            result = result.substring(0, idx)
                    + "root(" + x + "," + n + ")"
                    + result.substring(bounds[1] + 1);
        }
        return result;
    }

    private static String convertSqrt(String input) {
        String result = input;
        int idx;
        while ((idx = result.indexOf("\\sqrt{")) != -1) {
            int[] bounds = extractBraces(result, idx + 5);
            if (bounds == null) break;
            String x = convertSqrt(result.substring(bounds[0] + 1, bounds[1]));

            result = result.substring(0, idx)
                    + "sqrt(" + x + ")"
                    + result.substring(bounds[1] + 1);
        }
        return result;
    }

    private static String convertPowerBraces(String input) {
        String result = input;
        int idx;
        while ((idx = result.indexOf("^{")) != -1) {
            int[] bounds = extractBraces(result, idx + 1);
            if (bounds == null) break;
            String expr = convertPowerBraces(result.substring(bounds[0] + 1, bounds[1]));

            result = result.substring(0, idx)
                    + "^(" + expr + ")"
                    + result.substring(bounds[1] + 1);
        }
        return result;
    }

    private static String convertAbsValue(String input) {
        String result = input;
        int idx;
        while ((idx = result.indexOf("\\left|")) != -1) {
            int closeIdx = result.indexOf("\\right|", idx + 6);
            if (closeIdx == -1) break;
            String inner = result.substring(idx + 6, closeIdx);
            result = result.substring(0, idx)
                    + "abs(" + inner + ")"
                    + result.substring(closeIdx + 7);
        }
        return result;
    }

    private static String convertFunctionNames(String input) {
        String result = input;
        // Inverse trig — before regular to prevent partial match of "arcsin" → "arcMath.sin"
        result = result.replace("\\arcsin", "asin");
        result = result.replace("\\arccos", "acos");
        result = result.replace("\\arctan", "atan");
        result = result.replace("\\operatorname{arcsin}", "asin");
        result = result.replace("\\operatorname{arccos}", "acos");
        result = result.replace("\\operatorname{arctan}", "atan");
        // Hyperbolic — before regular trig (sinh before sin)
        result = result.replace("\\sinh", "sinh");
        result = result.replace("\\cosh", "cosh");
        result = result.replace("\\tanh", "tanh");
        // Regular trig
        result = result.replace("\\sin", "sin");
        result = result.replace("\\cos", "cos");
        result = result.replace("\\tan", "tan");
        result = result.replace("\\csc", "csc");
        result = result.replace("\\sec", "sec");
        result = result.replace("\\cot", "cot");
        // Log / exp
        result = result.replace("\\ln", "ln");
        result = result.replace("\\log", "lg");
        result = result.replace("\\exp", "exp");
        // Other
        result = result.replace("\\abs", "abs");
        return result;
    }

    private static String addImplicitMultiplication(String input) {
        StringBuilder sb = new StringBuilder(input);
        int i = 1;
        while (i < sb.length()) {
            char prev = sb.charAt(i - 1);
            char curr = sb.charAt(i);

            boolean insert = false;

            // digit/close-paren followed by letter: 2x → 2*x, )x → )*x
            if ((Character.isDigit(prev) || prev == ')') && Character.isLetter(curr)) {
                insert = !precedesFunction(sb, i);
            }
            // digit/close-paren followed by open-paren: 2( → 2*(, )( → )*(
            else if ((Character.isDigit(prev) || prev == ')') && curr == '(') {
                insert = true;
            }
            // letter followed by open-paren: x( → x*( but NOT sin( → sin*(
            else if (Character.isLetter(prev) && curr == '(') {
                insert = !precedesFunction(sb, i);
            }

            if (insert) {
                sb.insert(i, '*');
                i += 2;
            } else {
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * Returns true if the letters ending at {@code openParenPos} in {@code sb} form
     * a known function name, meaning we should NOT insert '*' before the '('.
     */
    private static boolean precedesFunction(StringBuilder sb, int openParenPos) {
        int wordEnd = openParenPos;
        int wordStart = openParenPos - 1;
        while (wordStart > 0 && Character.isLetter(sb.charAt(wordStart - 1))) {
            wordStart--;
        }
        if (wordStart >= wordEnd) return false;
        String word = sb.substring(wordStart, wordEnd).toLowerCase();
        return MathFunctionsRegistry.isFunction(word);
    }

    private static String swapZAndY(String formula) {
        StringBuilder result = new StringBuilder(formula.length());
        for (int i = 0; i < formula.length(); i++) {
            char c = formula.charAt(i);
            boolean prevAlphaNum = i > 0 && Character.isLetterOrDigit(formula.charAt(i - 1));
            boolean nextAlphaNum = i < formula.length() - 1 && Character.isLetterOrDigit(formula.charAt(i + 1));

            if (!prevAlphaNum && !nextAlphaNum) {
                if (c == 'y') result.append(ZY_SWAP_PLACEHOLDER);
                else if (c == 'z') result.append('y');
                else result.append(c);
            } else {
                result.append(c);
            }
        }
        return result.toString().replace(ZY_SWAP_PLACEHOLDER, 'z');
    }

    /**
     * Extracts the content of matching braces starting at {@code start} (which must be '{').
     *
     * @return [openPos, closePos] where content is s[openPos+1 .. closePos-1], or null if unmatched
     */
    private static int[] extractBraces(String s, int start) {
        if (start >= s.length() || s.charAt(start) != '{') return null;
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return new int[]{start, i};
        }
        return null;
    }
}
