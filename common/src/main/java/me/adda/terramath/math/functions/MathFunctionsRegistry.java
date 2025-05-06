package me.adda.terramath.math.functions;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final Map<String, String> FUNCTION_MAPPINGS = new ConcurrentHashMap<>();

    static {
        // Basic trigonometric functions
        FUNCTION_MAPPINGS.put("sin", "Math.sin");
        FUNCTION_MAPPINGS.put("cos", "Math.cos");
        FUNCTION_MAPPINGS.put("tan", "Math.tan");

        // Inverse trigonometric functions
        FUNCTION_MAPPINGS.put("asin", "Math.asin");
        FUNCTION_MAPPINGS.put("acos", "Math.acos");
        FUNCTION_MAPPINGS.put("atan", "Math.atan");
        FUNCTION_MAPPINGS.put("atan2", "Math.atan2");

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
        FUNCTION_MAPPINGS.put("root", "MathExtensions.root");

        // Random number generators
        FUNCTION_MAPPINGS.put("rand", "MathExtensions.rand");
        FUNCTION_MAPPINGS.put("randnormal", "MathExtensions.randnormal");
        FUNCTION_MAPPINGS.put("randrange", "MathExtensions.randrange");

        // Noises
        FUNCTION_MAPPINGS.put("perlin", "noise.getPerlinNoise");
        FUNCTION_MAPPINGS.put("simplex", "noise.getSimplexNoise");
        FUNCTION_MAPPINGS.put("normal", "noise.getNormalNoise");
        FUNCTION_MAPPINGS.put("blended", "noise.getBlendedNoise");
        FUNCTION_MAPPINGS.put("octaved", "noise.getOctavedNoise");
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
     * Get the sorted set of all supported function names.
     *
     * @return An unmodifiable set of function names
     */
    public static Set<String> getSortedFunctionNames() {
        List<String> sortedFunctionNames = new ArrayList<>(MathFunctionsRegistry.getFunctionNames());
        sortedFunctionNames.sort((f1, f2) -> {
            String impl1 = MathFunctionsRegistry.getFunctionImplementation(f1);
            String impl2 = MathFunctionsRegistry.getFunctionImplementation(f2);
            return Integer.compare(impl2.length(), impl1.length());
        });

        return new LinkedHashSet<>(sortedFunctionNames);
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