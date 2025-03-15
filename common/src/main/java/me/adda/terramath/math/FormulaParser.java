package me.adda.terramath.math;

import me.adda.terramath.exception.FormulaException;
import org.codehaus.janino.ExpressionEvaluator;

public class FormulaParser extends FormulaValidator {

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
                throw new FormulaException(ERROR_INVALID_SYNTAX, e.getMessage());
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
            System.out.println(e);
            String formatted_exception = e.getMessage().split(":")[1].trim();

            throw new FormulaException(ERROR_INVALID_SYNTAX, formatted_exception);
        }
    }

    private static String convertToJavaExpression(String formula) {
        String javaExpr = formula;

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
}