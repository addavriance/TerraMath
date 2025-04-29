package me.adda.terramath.math;

import java.util.*;

/**
 * Centralized registry for mathematical functions supported by the application.
 * This class maintains the mapping between function names and their implementations,
 * and provides utility methods for function validation.
 */
public class MathFunctionsRegistry {

    /**
     * Map of function names to their corresponding implementation method names.
     * This allows us to maintain a single source of truth for supported functions.
     */
    private static final Map<String, String> FUNCTION_MAPPINGS = new HashMap<>();

    static {
        // Basic trigonometric functions
        FUNCTION_MAPPINGS.put("sin", "Math.sin");
        FUNCTION_MAPPINGS.put("cos", "Math.cos");
        FUNCTION_MAPPINGS.put("tan", "Math.tan");

        // Inverse trigonometric functions
        FUNCTION_MAPPINGS.put("asin", "Math.asin");
        FUNCTION_MAPPINGS.put("acos", "Math.acos");
        FUNCTION_MAPPINGS.put("atan", "Math.atan");

        // Hyperbolic functions
        FUNCTION_MAPPINGS.put("sinh", "Math.sinh");
        FUNCTION_MAPPINGS.put("cosh", "Math.cosh");
        FUNCTION_MAPPINGS.put("tanh", "Math.tanh");

        // Root functions
        FUNCTION_MAPPINGS.put("sqrt", "Math.sqrt");
        FUNCTION_MAPPINGS.put("cbrt", "Math.cbrt");

        // Logarithmic and exponential functions
        FUNCTION_MAPPINGS.put("ln", "Math.log");
        FUNCTION_MAPPINGS.put("lg", "Math.log10");
        FUNCTION_MAPPINGS.put("exp", "Math.exp");

        // Power function
        FUNCTION_MAPPINGS.put("pow", "Math.pow");

        // Rounding functions
        FUNCTION_MAPPINGS.put("floor", "(int)Math.floor");
        FUNCTION_MAPPINGS.put("ceil", "Math.ceil");
        FUNCTION_MAPPINGS.put("round", "Math.round");

        // Miscellaneous functions
        FUNCTION_MAPPINGS.put("abs", "Math.abs");
        FUNCTION_MAPPINGS.put("sign", "Math.signum");

        // Special mathematical functions
        FUNCTION_MAPPINGS.put("gamma", "MathExtensions.gamma");
        FUNCTION_MAPPINGS.put("erf", "MathExtensions.erf");
        FUNCTION_MAPPINGS.put("beta", "MathExtensions.beta");

        // Additional utility functions
        FUNCTION_MAPPINGS.put("mod", "MathExtensions.mod");
        FUNCTION_MAPPINGS.put("max", "Math.max");
        FUNCTION_MAPPINGS.put("min", "Math.min");
        FUNCTION_MAPPINGS.put("sigmoid", "MathExtensions.sigmoid");
        FUNCTION_MAPPINGS.put("clamp", "MathExtensions.clamp");
        FUNCTION_MAPPINGS.put("gcd", "MathExtensions.gcd");
        FUNCTION_MAPPINGS.put("lcm", "MathExtensions.lcm");
        FUNCTION_MAPPINGS.put("modi", "MathExtensions.modInverse");

        // Random number generators
        FUNCTION_MAPPINGS.put("rand", "MathExtensions.rand");
        FUNCTION_MAPPINGS.put("randnormal", "MathExtensions.randnormal");
    }

    /**
     * Get the set of all supported function names.
     *
     * @return An unmodifiable set of function names
     */
    public static Set<String> getFunctionNames() {
        return Collections.unmodifiableSet(FUNCTION_MAPPINGS.keySet());
    }

    /**
     * Check if a function name is supported.
     *
     * @param functionName The function name to check
     * @return true if the function is supported, false otherwise
     */
    public static boolean isFunction(String functionName) {
        return FUNCTION_MAPPINGS.containsKey(functionName.toLowerCase());
    }

    /**
     * Get the implementation method name for a given function.
     *
     * @param functionName The function name
     * @return The corresponding implementation method name
     * @throws IllegalArgumentException if the function is not supported
     */
    public static String getFunctionImplementation(String functionName) {
        String implementation = FUNCTION_MAPPINGS.get(functionName.toLowerCase());
        if (implementation == null) {
            throw new IllegalArgumentException("Unsupported function: " + functionName);
        }
        return implementation;
    }
}