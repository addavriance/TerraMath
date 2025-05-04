package me.adda.terramath.math.parser;

import me.adda.terramath.exception.ExceptionUtils;
import me.adda.terramath.exception.FormulaException;
import me.adda.terramath.math.functions.CompositeNoise;
import me.adda.terramath.math.formula.FormulaValidator;
import me.adda.terramath.world.SeedUtils;
import org.codehaus.janino.ExpressionEvaluator;

import me.adda.terramath.math.formula.FormulaFormatter;


public class FormulaParser extends FormulaValidator {
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

        long seed = SeedUtils.getSeed();

        String javaExpression = FormulaFormatter.convertToJavaExpression(formula);

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
            Throwable cause = ExceptionUtils.getRootCause(e);
            String message = cause.getMessage();

            if (cause instanceof IllegalArgumentException ||
                    message != null && message.contains("No applicable constructor")) {
                throw new FormulaException(ERROR_INVALID_ARGUMENTS, ExceptionUtils.extractFunctionName(message));
            } else if (message != null && (message.contains("Unknown variable") || message.contains("is not an rvalue"))) {
                throw new FormulaException(ERROR_UNKNOWN_VARIABLE, ExceptionUtils.extractVariableName(message));
            } else {
                throw new FormulaException(ERROR_INVALID_SYNTAX, message);
            }
        }
    }
}