package me.adda.terramath.math.formula;

import me.adda.terramath.math.functions.MathFunctionsRegistry;

import java.util.*;

/**
 * Utility class for generating random mathematical formulas.
 * Designed for use with TerraMap mod to create random terrain functions.
 */
public class FormulaGenerator {
    private static final String[] OPERATORS = {"+", "-", "*", "/"};
    private static final String[] COORDINATES = {"x", "y", "z"};

    /**
     * Generates a random mathematical formula using functions from MathFunctionsRegistry.
     * Each formula will include at least one function, and use coordinates x, y, or z.
     *
     * @return A random formula string that can be parsed by FormulaParser
     */
    public static String generateRandomFormula() {
        Random random = new Random();
        List<String> availableFunctions = new ArrayList<>(MathFunctionsRegistry.getFunctionNames());
        Map<Integer, List<String>> functionsByArgCount = categorizeByArgCount(availableFunctions);

        StringBuilder formula = new StringBuilder();
        int terms = random.nextInt(3) + 2;
        boolean hasUsedFunction = false;

        for (int i = 0; i < terms; i++) {
            if (i > 0) {
                formula.append(" ").append(OPERATORS[random.nextInt(OPERATORS.length)]).append(" ");
            }

            int termType = random.nextInt(5);
            if (!hasUsedFunction && i == terms - 1) {
                termType = 0;
            }

            if (termType <= 2) {
                hasUsedFunction = true;
                generateFunctionTerm(formula, random, functionsByArgCount, COORDINATES, null, 0);
            } else {
                generatePolynomialTerm(formula, random, COORDINATES);
            }
        }

        return formula.toString();
    }

    /**
     * Generate a more complex formula with specific characteristics.
     *
     * @param complexity          Level of complexity (1-5, where 5 is most complex)
     * @param preferredCoordinate Preferred coordinate to emphasize (x, y, or z)
     * @return A complex formula with specified characteristics
     */
    public static String generateComplexFormula(int complexity, String preferredCoordinate) {
        Random random = new Random();

        boolean hasPreferredCoordinate = false;
        for (String coord : COORDINATES) {
            if (coord.equals(preferredCoordinate)) {
                hasPreferredCoordinate = true;
                break;
            }
        }

        if (!hasPreferredCoordinate) {
            preferredCoordinate = "x";
        }

        complexity = Math.max(1, Math.min(5, complexity));

        List<String> availableFunctions = new ArrayList<>(MathFunctionsRegistry.getSortedFunctionNames());
        Map<Integer, List<String>> functionsByArgCount = categorizeByArgCount(availableFunctions);

        StringBuilder formula = new StringBuilder();
        int terms = complexity + random.nextInt(2);

        for (int i = 0; i < terms; i++) {
            if (i > 0) {
                formula.append(" ").append(OPERATORS[random.nextInt(OPERATORS.length)]).append(" ");
            }

            int functionChance = 40 + (complexity * 10);
            if (random.nextInt(100) < functionChance) {
                int nestingLevel = random.nextInt(complexity / 2 + 1);
                generateFunctionTerm(formula, random, functionsByArgCount, COORDINATES,
                        preferredCoordinate, nestingLevel);
            } else {
                generatePolynomialTerm(formula, random, COORDINATES);
            }
        }

        return formula.toString();
    }

    /**
     * Categorizes functions by their expected argument count for better formula generation.
     */
    private static Map<Integer, List<String>> categorizeByArgCount(List<String> functions) {
        Map<Integer, List<String>> result = new HashMap<>();

        List<String> noArgFunctions = new ArrayList<>();
        List<String> singleArgFunctions = new ArrayList<>();
        List<String> twoArgFunctions = new ArrayList<>();
        List<String> threeArgFunctions = new ArrayList<>();

        for (String function : functions) {
            switch (function) {
                case "rand" -> noArgFunctions.add(function);
                case "pow", "max", "min", "beta", "mod", "randnormal", "randrange", "gcd", "lcm", "modi", "simplex" -> twoArgFunctions.add(function);
                case "clamp", "perlin", "blended", "normal" -> threeArgFunctions.add(function);
                case "octaved" -> {}
                default -> singleArgFunctions.add(function);
            }
        }

        result.put(0, noArgFunctions);
        result.put(1, singleArgFunctions);
        result.put(2, twoArgFunctions);
        result.put(3, threeArgFunctions);

        return result;
    }

    /**
     * Generates a function term with possible nesting.
     */
    private static void generateFunctionTerm(StringBuilder formula, Random random,
                                             Map<Integer, List<String>> functionsByArgCount,
                                             String[] coordinates, String preferredCoordinate,
                                             int nestingLevel) {
        int coefficient = random.nextInt(10);

        // Select argument count with weights
        int[] weights = {5, 70, 20, 5}; // Added weight for 3-argument functions
        int randomWeight = random.nextInt(100);
        int argCount;

        if (randomWeight < weights[0]) {
            argCount = 0;
        } else if (randomWeight < weights[0] + weights[1]) {
            argCount = 1;
        } else if (randomWeight < weights[0] + weights[1] + weights[2]) {
            argCount = 2;
        } else {
            argCount = 3;
        }

        // Fallback if no functions with this argument count exist
        if (!functionsByArgCount.containsKey(argCount) || functionsByArgCount.get(argCount).isEmpty()) {
            for (int i = 0; i <= 3; i++) {
                if (functionsByArgCount.containsKey(i) && !functionsByArgCount.get(i).isEmpty()) {
                    argCount = i;
                    break;
                }
            }
        }

        List<String> functions = functionsByArgCount.get(argCount);
        String function = functions.get(random.nextInt(functions.size()));

        if (coefficient > 1) {
            formula.append(coefficient).append("*");
        }

        formula.append(function).append("(");

        for (int i = 0; i < argCount; i++) {
            if (i > 0) {
                formula.append(", ");
            }

            if (nestingLevel > 0 && random.nextInt(3) == 0) {
                // Generate nested function
                generateFunctionTerm(formula, random, functionsByArgCount,
                        coordinates, preferredCoordinate, nestingLevel - 1);
            } else {
                // Generate simple argument
                generateArgument(formula, random, coordinates, preferredCoordinate);
            }
        }

        formula.append(")");
    }

    /**
     * Generates a function argument, which could be a simple coordinate or a more complex expression.
     */
    private static void generateArgument(StringBuilder formula, Random random,
                                         String[] coordinates, String preferredCoordinate) {
        String coordinate;
        if (preferredCoordinate != null && random.nextInt(3) > 0) {
            coordinate = preferredCoordinate;
        } else {
            coordinate = coordinates[random.nextInt(coordinates.length)];
        }

        int argType = random.nextInt(3);
        if (argType == 0) {
            formula.append(coordinate);
        } else if (argType == 1) {
            int coef = random.nextInt(9) + 2;
            formula.append(coef).append("*").append(coordinate);
        } else {
            int coef = random.nextInt(9) + 2;
            int constant = random.nextInt(10);
            boolean isAddition = random.nextBoolean();

            formula.append(coef).append("*").append(coordinate);
            formula.append(isAddition ? "+" : "-").append(constant);
        }
    }

    /**
     * Generates a polynomial term (ax^n + by^m + cz^k).
     */
    private static void generatePolynomialTerm(StringBuilder formula, Random random, String[] coordinates) {
        int coefficient = random.nextInt(9) + 1;
        String coordinate = coordinates[random.nextInt(coordinates.length)];
        int power = random.nextInt(3) + 1;

        formula.append(coefficient);

        formula.append("*").append(coordinate);
        if (power > 1) {
            formula.append("^").append(power);
        }
    }
}