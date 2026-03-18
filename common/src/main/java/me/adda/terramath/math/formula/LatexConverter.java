package me.adda.terramath.math.formula;

import me.adda.terramath.math.functions.MathFunctionsRegistry;

import java.util.*;
import java.util.regex.Pattern;

public class LatexConverter {

    private static final char ZY_SWAP_PLACEHOLDER = '\uE000';

    // Greek-letter map (longer keys first to avoid prefix collisions).
    private static final Map<String, String> GREEK = new LinkedHashMap<>();
    static {
        GREEK.put("\\varepsilon", "ε");
        GREEK.put("\\vartheta",   "θ");
        GREEK.put("\\varrho",     "ρ");
        GREEK.put("\\varphi",     "φ");
        GREEK.put("\\Upsilon",    "Υ");
        GREEK.put("\\upsilon",    "υ");
        GREEK.put("\\epsilon",    "ε");
        GREEK.put("\\lambda",     "λ");
        GREEK.put("\\Lambda",     "Λ");
        GREEK.put("\\Omega",      "Ω");
        GREEK.put("\\omega",      "ω");
        GREEK.put("\\Sigma",      "Σ");
        GREEK.put("\\sigma",      "σ");
        GREEK.put("\\Theta",      "Θ");
        GREEK.put("\\theta",      "θ");
        GREEK.put("\\alpha",      "α");
        GREEK.put("\\beta",       "β");
        GREEK.put("\\gamma",      "γ");
        GREEK.put("\\Gamma",      "Γ");
        GREEK.put("\\delta",      "δ");
        GREEK.put("\\Delta",      "Δ");
        GREEK.put("\\zeta",       "ζ");
        GREEK.put("\\iota",       "ι");
        GREEK.put("\\kappa",      "κ");
        GREEK.put("\\mu",         "μ");
        GREEK.put("\\nu",         "ν");
        GREEK.put("\\xi",         "ξ");
        GREEK.put("\\Xi",         "Ξ");
        GREEK.put("\\rho",        "ρ");
        GREEK.put("\\tau",        "τ");
        GREEK.put("\\chi",        "χ");
        GREEK.put("\\psi",        "ψ");
        GREEK.put("\\Psi",        "Ψ");
        GREEK.put("\\Phi",        "Φ");
        GREEK.put("\\phi",        "φ");
        GREEK.put("\\eta",        "η");
        GREEK.put("\\Pi",         "Π");
        GREEK.put("\\pi",         "π");
    }

    // Public API

    public static String convert(String latex) {
        if (latex == null || latex.trim().isEmpty()) return latex;
        String s = latex.trim();

        // 1. Strip math-mode delimiters ($...$ etc.)
        s = stripMathModeDelimiters(s);

        // 2. Strip function-definition prefix (f(x,y,z)= …)
        s = stripFunctionPrefix(s);

        // 3. Variant frac commands
        s = s.replace("\\dfrac", "\\frac");
        s = s.replace("\\tfrac", "\\frac");

        // 4. Strip size modifiers (\big, \Big, \bigg, \Bigg) before bracket processing
        s = stripSizeModifiers(s);

        // 5. Strip \text{...} annotations
        s = stripText(s);

        // 6. Handle \begin{...}...\end{...} environments (cases, matrix, …)
        s = convertEnvironments(s);

        // 7. Structural transforms (recursive / iteration-based)
        s = convertFrac(s);
        s = convertNthRoot(s);
        s = convertSqrt(s);
        s = convertOperatorname(s);

        // 8. \left...\right (must be before bare-|...|)
        s = convertLeftRight(s);

        // 9. Bare |x| -> abs(x)
        s = convertBareAbs(s);

        // 10. Superscripts ^{...} / ^x  and subscripts _{...} / _x
        s = convertPower(s);
        s = convertSubscript(s);

        // 11. Greek letters and math symbols
        s = convertGreekAndSymbols(s);

        // 12. LaTeX function names -> TerraMath names
        s = convertFunctionNames(s);

        // 13. LaTeX spacing and misc tokens
        s = stripSpacingCommands(s);

        // 14. Strip remaining bare braces
        s = s.replace("{", "").replace("}", "");

        // 15. Add parens to bare function calls: sin x -> sin(x)
        s = addFunctionParens(s);

        // 16. Remove all whitespace before implicit-mult pass
        s = s.replaceAll("\\s+", "");

        // 17. Split letter runs that aren't function names: xy -> x*y
        s = splitVariableRuns(s);

        // 18. Digit/close-paren adjacency: 2x -> 2*x, 2( -> 2*(, )( -> )*(
        s = addImplicitMultiplication(s);

        return s;
    }

    public static boolean isLatex(String input) {
        return input != null && input.contains("\\");
    }

    public static String swapZAndY(String formula) {
        StringBuilder result = new StringBuilder(formula.length());
        for (int i = 0; i < formula.length(); i++) {
            char c = formula.charAt(i);
            boolean prevAlNum = i > 0 && Character.isLetterOrDigit(formula.charAt(i - 1));
            boolean nextAlNum = i < formula.length() - 1 && Character.isLetterOrDigit(formula.charAt(i + 1));
            if (!prevAlNum && !nextAlNum) {
                if      (c == 'y') result.append(ZY_SWAP_PLACEHOLDER);
                else if (c == 'z') result.append('y');
                else               result.append(c);
            } else {
                result.append(c);
            }
        }
        return result.toString().replace(ZY_SWAP_PLACEHOLDER, 'z');
    }

    private static String stripMathModeDelimiters(String s) {
        if      (s.startsWith("\\[") && s.endsWith("\\]"))  s = s.substring(2, s.length() - 2).trim();
        else if (s.startsWith("$$")  && s.endsWith("$$"))   s = s.substring(2, s.length() - 2).trim();
        else if (s.startsWith("$")   && s.endsWith("$") && s.length() > 1) s = s.substring(1, s.length() - 1).trim();
        else if (s.startsWith("\\(") && s.endsWith("\\)"))  s = s.substring(2, s.length() - 2).trim();
        return s;
    }

    private static String stripFunctionPrefix(String latex) {
        // f\left(x,\ y\right)= …
        String r = latex.replaceAll("^[a-zA-Z]?\\\\left\\(.*?\\\\right\\)\\s*=\\s*", "");
        if (!r.equals(latex)) return r;
        // f(x,y,z)= …
        r = latex.replaceAll("^[a-zA-Z]?\\([^)]*\\)\\s*=\\s*", "");
        if (!r.equals(latex)) return r;
        // z= / y= / f= …
        return latex.replaceAll("^[a-zA-Z]\\s*=\\s*", "");
    }

    private static String stripSizeModifiers(String s) {
        // Order: longest first
        s = s.replace("\\bigg", "").replace("\\Bigg", "");
        s = s.replace("\\Big",  "").replace("\\big",  "");
        return s;
    }

    private static String stripText(String s) {
        // \text{anything} -> strip
        int idx;
        while ((idx = s.indexOf("\\text{")) != -1) {
            int[] b = extractBraces(s, idx + 6);
            if (b == null) break;
            s = s.substring(0, idx) + s.substring(b[1] + 1);
        }
        // \mathrm{e} -> e (Euler), generic \mathrm{x} -> x
        while ((idx = s.indexOf("\\mathrm{")) != -1) {
            int[] b = extractBraces(s, idx + 8);
            if (b == null) break;
            s = s.substring(0, idx) + s.substring(b[0] + 1, b[1]) + s.substring(b[1] + 1);
        }
        return s;
    }

    private static String convertEnvironments(String s) {
        // cases: \begin{cases} expr1 & cond \\ expr2 \end{cases}
        // We keep the full body (stripping begin/end/& markers) and replace \\ with +
        // as a best-effort approximation for a numeric evaluator.
        s = s.replaceAll("\\\\begin\\{cases\\}", "");
        s = s.replaceAll("\\\\end\\{cases\\}",   "");
        // matrix variants -> strip content (can't meaningfully evaluate a matrix)
        s = s.replaceAll("\\\\begin\\{[a-zA-Z]*matrix\\}[^\\\\]*\\\\end\\{[a-zA-Z]*matrix\\}", "0");
        // Generic begin/end
        s = s.replaceAll("\\\\begin\\{[^}]+\\}", "");
        s = s.replaceAll("\\\\end\\{[^}]+\\}",   "");
        // \\ (row separator in cases) -> space (the case expressions are separated)
        s = s.replace("\\\\", " ");
        // & (column separator) -> strip
        s = s.replace("&", " ");
        return s;
    }

    // \frac{a}{b} / \frac12 / \frac1{b} / \frac{a}2
    private static String convertFrac(String input) {
        String s = input;
        int idx;
        while ((idx = s.indexOf("\\frac")) != -1) {
            int after = skipSpaces(s, idx + 5);
            if (after >= s.length()) break;

            String num;
            int numEnd;
            if (s.charAt(after) == '{') {
                int[] b = extractBraces(s, after);
                if (b == null) break;
                num    = convertFrac(s.substring(b[0] + 1, b[1]));
                numEnd = b[1] + 1;
            } else {
                num    = String.valueOf(s.charAt(after));
                numEnd = after + 1;
            }

            int denStart = skipSpaces(s, numEnd);
            if (denStart >= s.length()) break;

            String den;
            int denEnd;
            if (s.charAt(denStart) == '{') {
                int[] b = extractBraces(s, denStart);
                if (b == null) break;
                den    = convertFrac(s.substring(b[0] + 1, b[1]));
                denEnd = b[1] + 1;
            } else {
                den    = String.valueOf(s.charAt(denStart));
                denEnd = denStart + 1;
            }

            s = s.substring(0, idx) + "(" + num + ")/(" + den + ")" + s.substring(denEnd);
        }
        return s;
    }

    private static String convertNthRoot(String input) {
        String s = input;
        int idx;
        while ((idx = s.indexOf("\\sqrt[")) != -1) {
            int close = s.indexOf(']', idx + 6);
            if (close == -1) break;
            String n = s.substring(idx + 6, close);
            int[] b = extractBraces(s, close + 1);
            if (b == null) break;
            String x = convertNthRoot(s.substring(b[0] + 1, b[1]));
            s = s.substring(0, idx) + "root(" + x + "," + n + ")" + s.substring(b[1] + 1);
        }
        return s;
    }

    // \sqrt{x} / \sqrt x / \sqrt2
    private static String convertSqrt(String input) {
        String s = input;
        int idx;
        while ((idx = s.indexOf("\\sqrt")) != -1) {
            int after = skipSpaces(s, idx + 5);
            if (after >= s.length()) break;
            String x;
            int end;
            if (s.charAt(after) == '{') {
                int[] b = extractBraces(s, after);
                if (b == null) break;
                x   = convertSqrt(s.substring(b[0] + 1, b[1]));
                end = b[1] + 1;
            } else {
                x   = String.valueOf(s.charAt(after));
                end = after + 1;
            }
            s = s.substring(0, idx) + "sqrt(" + x + ")" + s.substring(end);
        }
        return s;
    }

    // \operatorname{name} -> mapped name
    private static String convertOperatorname(String s) {
        int idx;
        while ((idx = s.indexOf("\\operatorname{")) != -1) {
            int[] b = extractBraces(s, idx + 14);
            if (b == null) break;
            String name = mapFunctionName(s.substring(b[0] + 1, b[1]));
            s = s.substring(0, idx) + name + s.substring(b[1] + 1);
        }
        return s;
    }

    private static String mapFunctionName(String name) {
        return switch (name.toLowerCase()) {
            case "arcsin"              -> "asin";
            case "arccos"              -> "acos";
            case "arctan"              -> "atan";
            case "arccsc"              -> "acsc";
            case "arcsec"              -> "asec";
            case "arccot"              -> "acot";
            case "arcsinh", "asinh"    -> "asinh";
            case "arccosh", "acosh"    -> "acosh";
            case "arctanh", "atanh"    -> "atanh";
            case "log"                 -> "lg";
            case "sgn", "sign"         -> "sign";
            case "ceiling"             -> "ceil";
            default -> MathFunctionsRegistry.isFunction(name.toLowerCase()) ? name.toLowerCase() : name;
        };
    }

    // All \left...\right variants
    private static String convertLeftRight(String s) {
        // \left|...\right| -> abs(...)
        s = convertMatchedLeftRight(s, "\\left|", "\\right|", "abs(", ")");
        // \left\|...\right\| (norm) -> abs(...)
        s = convertMatchedLeftRight(s, "\\left\\|", "\\right\\|", "abs(", ")");
        // \left\lfloor...\right\rfloor -> floor(...)
        s = convertMatchedLeftRight(s, "\\left\\lfloor", "\\right\\rfloor", "floor(", ")");
        // \left\lceil...\right\rceil -> ceil(...)
        s = convertMatchedLeftRight(s, "\\left\\lceil", "\\right\\rceil", "ceil(", ")");
        // Invisible delimiters (dot): \left. and \right. -> strip
        s = s.replace("\\left.",  "");
        s = s.replace("\\right.", "");
        // Remaining \left/\right with any bracket
        s = s.replace("\\left(",    "(").replace("\\right)",  ")");
        s = s.replace("\\left[",    "(").replace("\\right]",  ")");
        s = s.replace("\\left\\{",  "(").replace("\\right\\}",")" );
        s = s.replace("\\left<",    "(").replace("\\right>",  ")");
        s = s.replace("\\left\\langle", "(").replace("\\right\\rangle", ")");
        // Catch-all (strips any remaining \left / \right)
        s = s.replace("\\left",  "").replace("\\right", "");
        return s;
    }

    private static String convertMatchedLeftRight(String s, String open, String close,
                                                   String openRepl, String closeRepl) {
        int idx;
        while ((idx = s.indexOf(open)) != -1) {
            int closeIdx = s.indexOf(close, idx + open.length());
            if (closeIdx == -1) break;
            String inner = s.substring(idx + open.length(), closeIdx);
            s = s.substring(0, idx) + openRepl + inner + closeRepl + s.substring(closeIdx + close.length());
        }
        return s;
    }

    // Bare |x| (without \left) — alternating open/close
    private static String convertBareAbs(String s) {
        StringBuilder result = new StringBuilder(s.length());
        boolean inAbs = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '|') {
                if (!inAbs) { result.append("abs("); inAbs = true;  }
                else        { result.append(')');     inAbs = false; }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    // ^{expr} / ^x (single char) — skips already-converted ^(...)
    private static String convertPower(String input) {
        String s = input;
        int from = 0, idx;
        while ((idx = s.indexOf('^', from)) != -1) {
            int after = skipSpaces(s, idx + 1);
            if (after >= s.length()) break;
            char c = s.charAt(after);
            if (c == '(') { from = after + 1; continue; } // already processed
            String expr;
            int end;
            if (c == '{') {
                int[] b = extractBraces(s, after);
                if (b == null) { from = idx + 1; continue; }
                expr = convertPower(s.substring(b[0] + 1, b[1]));
                end  = b[1] + 1;
            } else {
                expr = String.valueOf(c);
                end  = after + 1;
            }
            s    = s.substring(0, idx) + "^(" + expr + ")" + s.substring(end);
            from = idx + expr.length() + 3;
        }
        return s;
    }

    // _{digits} -> append digits; _{anything} -> drop
    private static String convertSubscript(String input) {
        String s = input;
        int from = 0, idx;
        while ((idx = s.indexOf('_', from)) != -1) {
            int after = skipSpaces(s, idx + 1);
            if (after >= s.length()) { s = s.substring(0, idx); break; }
            String sub;
            int end;
            if (s.charAt(after) == '{') {
                int[] b = extractBraces(s, after);
                if (b == null) { from = idx + 1; continue; }
                sub = s.substring(b[0] + 1, b[1]);
                end = b[1] + 1;
            } else {
                sub = String.valueOf(s.charAt(after));
                end = after + 1;
            }
            // Keep only digit subscripts (x_1 -> x1), drop others (x_n -> x)
            String keep = sub.matches("[0-9]+") ? sub : "";
            s    = s.substring(0, idx) + keep + s.substring(end);
            from = idx + keep.length();
        }
        return s;
    }

    private static String convertGreekAndSymbols(String s) {
        for (Map.Entry<String, String> e : GREEK.entrySet()) {
            s = s.replace(e.getKey(), e.getValue());
        }
        // Arithmetic operators
        s = s.replace("\\cdot",   "*");
        s = s.replace("\\times",  "*");
        s = s.replace("\\div",    "/");
        s = s.replace("\\pm",     "+");
        s = s.replace("\\mp",     "-");
        // Comparisons
        s = s.replace("\\leq",    "<=");
        s = s.replace("\\le",     "<=");
        s = s.replace("\\geq",    ">=");
        s = s.replace("\\ge",     ">=");
        s = s.replace("\\neq",    "!=");
        s = s.replace("\\ne",     "!=");
        // Constants
        s = s.replace("\\infty",  "1e300");
        s = s.replace("\\euler",  "e");
        // Summation / product / integral — strip limits, leave body
        s = s.replaceAll("\\\\sum(_\\{[^}]*\\})?(\\^\\{[^}]*\\})?",   "");
        s = s.replaceAll("\\\\prod(_\\{[^}]*\\})?(\\^\\{[^}]*\\})?",  "");
        s = s.replaceAll("\\\\int(_\\{[^}]*\\})?(\\^\\{[^}]*\\})?",   "");
        s = s.replaceAll("\\\\iint(_\\{[^}]*\\})?(\\^\\{[^}]*\\})?",  "");
        s = s.replaceAll("\\\\iiint(_\\{[^}]*\\})?(\\^\\{[^}]*\\})?", "");
        s = s.replaceAll("\\\\oint(_\\{[^}]*\\})?(\\^\\{[^}]*\\})?",  "");
        s = s.replaceAll("\\\\lim(_\\{[^}]*\\})?",                     "");
        s = s.replace("\\nabla",   "");
        s = s.replace("\\partial", "");
        // Dots
        s = s.replace("\\cdots",   "");
        s = s.replace("\\ldots",   "");
        s = s.replace("\\dots",    "");
        return s;
    }

    private static String convertFunctionNames(String s) {
        // Longer / more specific names first to avoid prefix collisions
        // Inverse hyperbolic
        s = s.replace("\\arcsinh", "asinh");
        s = s.replace("\\arccosh", "acosh");
        s = s.replace("\\arctanh", "atanh");
        // Inverse trig (before trig)
        s = s.replace("\\arcsin", "asin");
        s = s.replace("\\arccos", "acos");
        s = s.replace("\\arctan", "atan");
        s = s.replace("\\arccsc", "acsc");
        s = s.replace("\\arcsec", "asec");
        s = s.replace("\\arccot", "acot");
        // Hyperbolic (before plain trig — sinh before sin)
        s = s.replace("\\sinh", "sinh"); s = s.replace("\\cosh", "cosh");
        s = s.replace("\\tanh", "tanh"); s = s.replace("\\csch", "csch");
        s = s.replace("\\sech", "sech"); s = s.replace("\\coth", "coth");
        s = s.replace("\\asinh", "asinh"); s = s.replace("\\acosh", "acosh");
        s = s.replace("\\atanh", "atanh");
        // Plain trig
        s = s.replace("\\sin", "sin"); s = s.replace("\\cos", "cos");
        s = s.replace("\\tan", "tan"); s = s.replace("\\csc", "csc");
        s = s.replace("\\sec", "sec"); s = s.replace("\\cot", "cot");
        // Log / exp
        s = s.replace("\\ln",  "ln");
        s = s.replace("\\log", "lg");
        s = s.replace("\\exp", "exp");
        s = s.replace("\\lg",  "lg");
        // Misc
        s = s.replace("\\abs",   "abs");
        s = s.replace("\\max",   "max");
        s = s.replace("\\min",   "min");
        s = s.replace("\\gcd",   "gcd");
        s = s.replace("\\lcm",   "lcm");
        s = s.replace("\\sgn",   "sign");
        s = s.replace("\\sign",  "sign");
        s = s.replace("\\floor", "floor");
        s = s.replace("\\ceil",  "ceil");
        s = s.replace("\\mod",   "mod");
        s = s.replace("\\sqrt",  "sqrt"); // catch-all (bare \sqrt after convertSqrt)
        return s;
    }

    private static String stripSpacingCommands(String s) {
        s = s.replace("\\ ",    " ");
        s = s.replace("\\,",    " ");
        s = s.replace("\\;",    " ");
        s = s.replace("\\:",    " ");
        s = s.replace("\\!",    "");
        s = s.replace("\\quad", " ");
        s = s.replace("\\qquad"," ");
        return s;
    }

    private static String addFunctionParens(String s) {
        // Sort by length descending so longer names match first
        List<String> fns = new ArrayList<>(MathFunctionsRegistry.getFunctionNames());
        fns.sort((a, b) -> b.length() - a.length());

        for (String fn : fns) {
            // Match: word-boundary fn, spaces, then a simple alphanumeric/Greek token
            // NOT followed by '(' (already has parens)
            String pat = "(?<![a-zA-Z])" + Pattern.quote(fn)
                    + "\\s+([0-9]*\\.?[0-9]+|[a-zA-Z\u03B1-\u03C9\u0391-\u03A9φπθ])(?![a-zA-Z0-9(])";
            s = s.replaceAll(pat, fn + "($1)");
        }
        return s;
    }

    private static String splitVariableRuns(String s) {
        StringBuilder result = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            // Only split plain ASCII letter runs (Greek/Unicode single chars are fine as-is)
            if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z') {
                int start = i;
                while (i < s.length() && (s.charAt(i) >= 'a' && s.charAt(i) <= 'z'
                        || s.charAt(i) >= 'A' && s.charAt(i) <= 'Z')) {
                    i++;
                }
                String word = s.substring(start, i);
                boolean followedByParen = i < s.length() && s.charAt(i) == '(';
                if (followedByParen || MathFunctionsRegistry.isFunction(word.toLowerCase())) {
                    result.append(word);
                } else if (word.length() == 1) {
                    result.append(word);
                } else {
                    // Split — but respect any embedded function name prefix/suffix
                    // Simple approach: split one letter at a time
                    for (int j = 0; j < word.length(); j++) {
                        if (j > 0) result.append('*');
                        result.append(word.charAt(j));
                    }
                }
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }

    private static String addImplicitMultiplication(String input) {
        StringBuilder sb = new StringBuilder(input);
        int i = 1;
        while (i < sb.length()) {
            char prev = sb.charAt(i - 1);
            char curr = sb.charAt(i);
            boolean insert = false;

            if ((Character.isDigit(prev) || prev == ')') && Character.isLetter(curr)) {
                insert = !precedesFunction(sb, i);
            } else if ((Character.isDigit(prev) || prev == ')') && curr == '(') {
                insert = true;
            } else if (Character.isLetter(prev) && curr == '(') {
                insert = !precedesFunction(sb, i);
            }

            if (insert) { sb.insert(i, '*'); i += 2; }
            else          i++;
        }
        return sb.toString();
    }

    private static boolean precedesFunction(StringBuilder sb, int openParenPos) {
        int end   = openParenPos;
        int start = openParenPos - 1;
        while (start > 0 && Character.isLetter(sb.charAt(start - 1))) start--;
        if (start >= end) return false;
        return MathFunctionsRegistry.isFunction(sb.substring(start, end).toLowerCase());
    }

    private static int[] extractBraces(String s, int start) {
        if (start >= s.length() || s.charAt(start) != '{') return null;
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if      (c == '{') depth++;
            else if (c == '}' && --depth == 0) return new int[]{start, i};
        }
        return null;
    }

    private static int skipSpaces(String s, int i) {
        while (i < s.length() && s.charAt(i) == ' ') i++;
        return i;
    }
}
