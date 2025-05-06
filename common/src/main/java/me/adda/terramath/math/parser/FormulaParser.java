package me.adda.terramath.math.parser;

import me.adda.terramath.exception.ExceptionUtils;
import me.adda.terramath.exception.FormulaException;
import me.adda.terramath.math.functions.CompositeNoise;
import me.adda.terramath.math.formula.FormulaValidator;
import me.adda.terramath.notification.NotificationManager;
import me.adda.terramath.world.SeedUtils;
import net.minecraft.client.Minecraft;
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

    private static ExpressionEvaluator createEvaluator() {
        ExpressionEvaluator evaluator = new ExpressionEvaluator();

        evaluator.setParameters(
                new String[]{"x", "y", "z", "noise"},
                new Class[]{double.class, double.class, double.class, CompositeNoise.class}
        );

        evaluator.setParentClassLoader(FormulaParser.class.getClassLoader());
        evaluator.setExpressionType(double.class);

        return evaluator;
    }

    public static CompiledFormula parse(String formula) {
        String javaExpression = FormulaFormatter.convertToJavaExpression(formula);

        long seed = SeedUtils.getSeed();
        CompositeNoise noise = new CompositeNoise(seed);

        ExpressionEvaluator evaluator = createEvaluator();

        try {

            if (!FormulaValidator.validateFormula(formula).isValid()) {
                throw new Exception("Invalid formula");
            }

            String fullExpression = String.format(
                    "import me.adda.terramath.math.functions.MathExtensions; " +
                    "(%s)",
                    javaExpression
            );

            evaluator.cook(fullExpression);
            return new CompiledFormula(formula, evaluator, noise);
        } catch (Exception e) {
            Throwable cause = ExceptionUtils.getRootCause(e);
            String message = cause.getMessage();

            if (Minecraft.getInstance().level != null) {
                NotificationManager.showFormulaError(formula);

                return new CompiledFormula(formula, evaluator, noise);
            }

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