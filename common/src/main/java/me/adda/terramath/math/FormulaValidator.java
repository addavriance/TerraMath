package me.adda.terramath.math;

import me.adda.terramath.exception.FormulaException;
import java.util.*;

public class FormulaValidator {
    public static final String TRANSLATION_PREFIX = "terramath.formula.error.";
    public static final String ERROR_NULL = TRANSLATION_PREFIX + "null";
    public static final String ERROR_INVALID_CHARS = TRANSLATION_PREFIX + "invalid_chars";
    public static final String ERROR_NO_VARIABLES = TRANSLATION_PREFIX + "no_variables";
    public static final String ERROR_UNKNOWN_FUNCTION = TRANSLATION_PREFIX + "unknown_function";
    public static final String ERROR_FUNCTION_PARENTHESES = TRANSLATION_PREFIX + "function_parentheses";
    public static final String ERROR_UNMATCHED_CLOSING = TRANSLATION_PREFIX + "unmatched_closing";
    public static final String ERROR_EMPTY_BRACKETS = TRANSLATION_PREFIX + "empty_brackets";
    public static final String ERROR_UNMATCHED_OPENING = TRANSLATION_PREFIX + "unmatched_opening";
    public static final String ERROR_OPERATOR_SEQUENCE = TRANSLATION_PREFIX + "operator_sequence";
    public static final String ERROR_OPERATOR_START_END = TRANSLATION_PREFIX + "operator_start_end";
    public static final String ERROR_OPERATOR_BRACKETS = TRANSLATION_PREFIX + "operator_brackets";
    public static final String ERROR_DIVISION_ZERO = TRANSLATION_PREFIX + "division_zero";
    public static final String ERROR_INVALID_SYNTAX = TRANSLATION_PREFIX + "invalid_syntax";

    protected static final Set<String> FUNCTIONS = new HashSet<>(Arrays.asList(
            "sin", "cos", "tan", "asin", "acos", "atan",
            "sinh", "cosh", "tanh", "asinh", "acosh", "atanh",
            "sqrt", "cbrt", "pow",
            "ln", "lg", "abs", "exp", "floor", "ceil",
            "round", "sign", "gamma", "erf", "beta", "mod",
            "max", "min", "sigmoid", "clamp",
            "csc", "cot", "sec", "acsc", "acot", "asec",
            "csch", "coth", "sech", "acsch", "acoth", "asech"
    ));

    public record ValidationResult(boolean isValid, String errorKey, Object... errorArgs) {
    }

    public static ValidationResult validateFormula(String formula) {
        if (formula == null) {
            return new ValidationResult(false, ERROR_NULL);
        }

        formula = formula.trim();
        if (formula.isEmpty()) {
            return new ValidationResult(false, ERROR_NO_VARIABLES);
        }

        try {
            validateBasicStructure(formula);
            validateFunctionsAndBrackets(formula);
            validateOperators(formula);
            testFormula(formula);
            return new ValidationResult(true, null);
        } catch (FormulaException e) {
            return new ValidationResult(false, e.getMessage(), e.getArgs());
        } catch (IllegalArgumentException e) {
            return new ValidationResult(false, e.getMessage());
        }
    }

    private static void validateBasicStructure(String formula) {
        if (!formula.matches("^[\\sxyz\\d+\\-*/(),.!^sincoatqrpwbdelhfgum]+$")) {
            throw new IllegalArgumentException(ERROR_INVALID_CHARS);
        }

        if (!formula.contains("x") && !formula.contains("y") && !formula.contains("z")) {
            throw new IllegalArgumentException(ERROR_NO_VARIABLES);
        }
    }

    private static void validateFunctionsAndBrackets(String formula) {
        Stack<Integer> bracketStack = new Stack<>();
        StringBuilder currentFunction = new StringBuilder();
        boolean inFunction = false;

        for (int i = 0; i < formula.length(); i++) {
            char c = formula.charAt(i);

            if (Character.isLetter(c)) {
                currentFunction.append(c);
                inFunction = true;
            } else if (inFunction) {
                String func = currentFunction.toString().toLowerCase();
                if (!FUNCTIONS.contains(func) && c == '(') {
                    throw new FormulaException(ERROR_UNKNOWN_FUNCTION, func);
                }
                currentFunction = new StringBuilder();
                inFunction = false;

                if (FUNCTIONS.contains(func) && c != '(') {
                    throw new FormulaException(ERROR_FUNCTION_PARENTHESES, func);
                }
            }

            if (c == '(') {
                bracketStack.push(i);
            } else if (c == ')') {
                if (bracketStack.isEmpty()) {
                    throw new FormulaException(ERROR_UNMATCHED_CLOSING, i);
                }
                int openPos = bracketStack.pop();
                if (i - openPos == 1) {
                    throw new FormulaException(ERROR_EMPTY_BRACKETS, openPos);
                }
            }
        }

        if (!bracketStack.isEmpty()) {
            throw new FormulaException(ERROR_UNMATCHED_OPENING, bracketStack.peek());
        }
    }

    private static void validateOperators(String formula) {
        if (formula.matches(".*[+\\-*/]{2,}.*")) {
            throw new IllegalArgumentException(ERROR_OPERATOR_SEQUENCE);
        }

        if (formula.matches("^[*/].*") || formula.matches(".*[+\\-*/]$")) {
            throw new IllegalArgumentException(ERROR_OPERATOR_START_END);
        }

        if (formula.matches(".*\\([+*/].*") || formula.matches(".*[+\\-*/]\\).*")) {
            throw new IllegalArgumentException(ERROR_OPERATOR_BRACKETS);
        }
    }

    private static void testFormula(String formula) {
        FormulaParser.CompiledFormula expression = FormulaParser.parse(formula);

        expression.evaluate(0.1, 0.1, 0.1);
    }

    protected static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '^';
    }
}