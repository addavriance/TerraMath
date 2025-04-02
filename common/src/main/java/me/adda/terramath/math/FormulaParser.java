package me.adda.terramath.math;

import me.adda.terramath.exception.FormulaException;
import org.codehaus.janino.ExpressionEvaluator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormulaParser extends FormulaValidator {
    private static final Map<String, String> FUNCTION_MAPPINGS = new ConcurrentHashMap<>();
    static {
        FUNCTION_MAPPINGS.put("sin", "Math.sin");
        FUNCTION_MAPPINGS.put("cos", "Math.cos");
        FUNCTION_MAPPINGS.put("tan", "Math.tan");
        FUNCTION_MAPPINGS.put("asin", "Math.asin");
        FUNCTION_MAPPINGS.put("acos", "Math.acos");
        FUNCTION_MAPPINGS.put("atan", "Math.atan");
        FUNCTION_MAPPINGS.put("sinh", "Math.sinh");
        FUNCTION_MAPPINGS.put("cosh", "Math.cosh");
        FUNCTION_MAPPINGS.put("tanh", "Math.tanh");
        FUNCTION_MAPPINGS.put("sqrt", "Math.sqrt");
        FUNCTION_MAPPINGS.put("cbrt", "Math.cbrt");
        FUNCTION_MAPPINGS.put("pow", "Math.pow");
        FUNCTION_MAPPINGS.put("ln", "Math.log");
        FUNCTION_MAPPINGS.put("lg", "Math.log10");
        FUNCTION_MAPPINGS.put("abs", "Math.abs");
        FUNCTION_MAPPINGS.put("exp", "Math.exp");
        FUNCTION_MAPPINGS.put("floor", "Math.floor");
        FUNCTION_MAPPINGS.put("ceil", "Math.ceil");
        FUNCTION_MAPPINGS.put("round", "(double)Math.round");
        FUNCTION_MAPPINGS.put("sign", "Math.signum");
        FUNCTION_MAPPINGS.put("max", "Math.max");
        FUNCTION_MAPPINGS.put("min", "Math.min");

        FUNCTION_MAPPINGS.put("gamma", "MathExtensions.gamma");
        FUNCTION_MAPPINGS.put("erf", "MathExtensions.erf");
        FUNCTION_MAPPINGS.put("beta", "MathExtensions.beta");
        FUNCTION_MAPPINGS.put("mod", "MathExtensions.mod");
        FUNCTION_MAPPINGS.put("sigmoid", "MathExtensions.sigmoid");
        FUNCTION_MAPPINGS.put("clamp", "MathExtensions.clamp");

        FUNCTION_MAPPINGS.put("sec", "MathExtensions.sec");
        FUNCTION_MAPPINGS.put("csc", "MathExtensions.csc");
        FUNCTION_MAPPINGS.put("cot", "MathExtensions.cot");
    }

    public static class CompiledFormula {
        private final String originalExpression;
        private final ExpressionEvaluator evaluator;

        private CompiledFormula(String expression, ExpressionEvaluator evaluator) {
            this.originalExpression = expression;
            this.evaluator = evaluator;

        }

        public double evaluate(double x, double y, double z) {
            try {
                return (double) evaluator.evaluate(new Object[]{x, y, z});
            } catch (Exception e) {
                throw new FormulaException(ERROR_INVALID_CHARS, e.getMessage());
            }
        }

        public String getOriginalExpression() {
            return originalExpression;
        }
    }

    public static CompiledFormula parse(String formula) {
        String javaExpression = convertToJavaExpression(formula);

        try {
            ExpressionEvaluator evaluator = new ExpressionEvaluator();

            evaluator.setParameters(
                    new String[]{"x", "y", "z"},
                    new Class[]{double.class, double.class, double.class}
            );

            evaluator.setParentClassLoader(FormulaParser.class.getClassLoader());
            evaluator.setExpressionType(double.class);

            String fullExpression = String.format(
                    "import me.adda.terramath.math.MathExtensions; " +
                    "(%s)",
                    javaExpression
            );

            evaluator.cook(fullExpression);
            return new CompiledFormula(formula, evaluator);
        } catch (Exception e) {
            String formatted_exception = e.getMessage().split(":")[1].trim();
            formatted_exception = formatted_exception.length() > 43 ? formatted_exception.substring(0, 43).trim() + "..." : formatted_exception;

            throw new FormulaException(ERROR_INVALID_SYNTAX, formatted_exception);
        }
    }

    private static String convertToJavaExpression(String formula) {
        // Replace function names with their Java equivalents
        String javaExpr = formula;
        for (Map.Entry<String, String> entry : FUNCTION_MAPPINGS.entrySet()) {
            javaExpr = javaExpr.replaceAll(
                    "\\b" + entry.getKey() + "\\b",
                    entry.getValue()
            );
        }

        if (javaExpr.contains("!")) {
            javaExpr = handleFactorials(javaExpr);
        }

        javaExpr = replacePowerOperator(javaExpr);

        return javaExpr;
    }

    private static String replacePowerOperator(String expr) {
        Pattern pattern = Pattern.compile("(\\w+)\\s*\\^\\s*(\\d+)");
        Matcher matcher = pattern.matcher(expr);

        while (matcher.find()) {
            String base = matcher.group(1);
            String exponent = matcher.group(2);
            String replacement = "Math.pow(" + base + ", " + exponent + ")";
            expr = expr.replace(matcher.group(0), replacement);
        }

        return expr;
    }

    private static String handleFactorials(String expr) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < expr.length()) {
            if (expr.charAt(i) == '!' && i > 0) {
                int j = i - 1;
                int parenthesesCount = 0;
                while (j >= 0) {
                    char c = expr.charAt(j);
                    if (c == ')') parenthesesCount++;
                    if (c == '(') parenthesesCount--;
                    if (parenthesesCount == 0 && isOperator(c)) break;
                    j--;
                }

                j++;

                String operand = expr.substring(j, i);
                result.delete(result.length() - operand.length(), result.length());
                result.append("MathExtensions.gamma(").append(operand).append(" + 1)");

            } else {
                result.append(expr.charAt(i));
            }

            i++;
        }
        return result.toString();
    }
}