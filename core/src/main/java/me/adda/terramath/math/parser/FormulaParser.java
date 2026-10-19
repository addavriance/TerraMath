package me.adda.terramath.math.parser;

import me.adda.terramath.TerraMath;
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
    public interface Evaluator {
        double eval(double x, double y, double z, CompositeNoise noise);
    }

    public static class CompiledFormula {
        private static final ThreadLocal<CompositeNoise> THREAD_VIEW = new ThreadLocal<>();

        private final String originalExpression;
        private final Evaluator evaluator;
        private final CompositeNoise noise;

        private CompiledFormula(String expression, Evaluator evaluator, CompositeNoise noise) {
            this.originalExpression = expression;
            this.evaluator = evaluator;
            this.noise = noise;
        }

        public double evaluate(double x, double y, double z) {
            try {
                CompositeNoise view = THREAD_VIEW.get();
                if (view == null || !view.isViewOf(noise)) {
                    view = noise.threadView();
                    THREAD_VIEW.set(view);
                }
                return evaluator.eval(x, y, z, view);
            } catch (FormulaException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new FormulaException(ERROR_INVALID_SYNTAX, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }
        }

        public String getOriginalExpression() {
            return originalExpression;
        }
    }

    private static final Evaluator NOT_COMPILED = (x, y, z, noise) -> {
        throw new FormulaException(ERROR_INVALID_SYNTAX, "Formula is not compiled");
    };

    private static ExpressionEvaluator createEvaluator() {
        ExpressionEvaluator evaluator = new ExpressionEvaluator();
        evaluator.setDefaultImports("me.adda.terramath.math.functions.MathExtensions");
        evaluator.setParentClassLoader(FormulaParser.class.getClassLoader());
        return evaluator;
    }

    private static boolean isRuntime() {
        try {
            return Minecraft.getInstance().level != null;
        } catch (Throwable t) {
            return true;
        }
    }

    private static void reportBrokenFormula(String formula) {
        try {
            NotificationManager.showFormulaError(formula);
        } catch (Throwable t) {
            TerraMath.LOGGER.error("Terrain formula could not be compiled: {}", formula);
        }
    }

    public static CompiledFormula tryParse(String formula) {
        try {
            if (!FormulaValidator.validateFormula(formula, true).isValid()) {
                return null;
            }
            String javaExpression = FormulaFormatter.convertToJavaExpression(formula);
            Evaluator compiled = (Evaluator) createEvaluator().createFastEvaluator(
                    "(" + javaExpression + ")",
                    Evaluator.class,
                    new String[]{"x", "y", "z", "noise"}
            );
            return new CompiledFormula(formula, compiled, new CompositeNoise(SeedUtils.getSeed()));
        } catch (Exception e) {
            return null;
        }
    }

    public static CompiledFormula parse(String formula) {
        String javaExpression = FormulaFormatter.convertToJavaExpression(formula);

        long seed = SeedUtils.getSeed();
        CompositeNoise noise = new CompositeNoise(seed);

        ExpressionEvaluator evaluator = createEvaluator();

        try {

            if (!FormulaValidator.validateFormula(formula, true).isValid()) {
                throw new Exception("Invalid formula");
            }

            Evaluator compiled = (Evaluator) evaluator.createFastEvaluator(
                    "(" + javaExpression + ")",
                    Evaluator.class,
                    new String[]{"x", "y", "z", "noise"}
            );
            return new CompiledFormula(formula, compiled, noise);
        } catch (Exception e) {
            Throwable cause = ExceptionUtils.getRootCause(e);
            String message = cause.getMessage();

            if (isRuntime()) {
                reportBrokenFormula(formula);

                return new CompiledFormula(formula, NOT_COMPILED, noise);
            }

            if (cause instanceof IllegalArgumentException ||
                    message != null && message.contains("No applicable constructor")) {
                throw new FormulaException(ERROR_INVALID_ARGUMENTS, ExceptionUtils.extractFunctionName(message));
            } else if (message != null && (message.contains("Unknown variable") || message.contains("is not an rvalue"))) {
                throw new FormulaException(ERROR_UNKNOWN_VARIABLE, ExceptionUtils.extractVariableName(message));
            } else {
                throw new FormulaException(ERROR_INVALID_SYNTAX, ExceptionUtils.extractSyntaxError(message));
            }
        }
    }
}