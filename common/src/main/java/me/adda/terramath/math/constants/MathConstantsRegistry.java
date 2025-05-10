package me.adda.terramath.math.constants;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MathConstantsRegistry {

    /**
     * Map of constant names to their corresponding implementation method names.
     * This allows us to maintain a single source of truth for supported constants.
     */
    private static final Map<String, String> CONSTANT_MAPPINGS = new ConcurrentHashMap<>();

    static {
        // Mathematical Constants
        CONSTANT_MAPPINGS.put("π", "Math.PI");
        CONSTANT_MAPPINGS.put("pi", "Math.PI");

        CONSTANT_MAPPINGS.put("e", "Math.E");

        CONSTANT_MAPPINGS.put("φ", "1.618033988749894848204");
        CONSTANT_MAPPINGS.put("phi", "1.618033988749894848204");

        // Special Mathematical Constants
        CONSTANT_MAPPINGS.put("ζ3", "1.2020569031595942");
        CONSTANT_MAPPINGS.put("zeta3", "1.2020569031595942");

        CONSTANT_MAPPINGS.put("K", "0.91596559417721901");
        CONSTANT_MAPPINGS.put("catalan", "0.91596559417721901");

        // Feigenbaum Constants
        CONSTANT_MAPPINGS.put("α", "2.5029078750958928");
        CONSTANT_MAPPINGS.put("alpha", "2.5029078750958928");
        CONSTANT_MAPPINGS.put("feigenbaum", "2.5029078750958928");

        CONSTANT_MAPPINGS.put("δ", "4.6692016091029906");
        CONSTANT_MAPPINGS.put("delta", "4.6692016091029906");
        CONSTANT_MAPPINGS.put("feigenbaumdelta", "4.6692016091029906");

        // Physical and Cosmological Constants
        CONSTANT_MAPPINGS.put("Ω", "0.6889");
        CONSTANT_MAPPINGS.put("omega", "0.6889");
    }

    /**
     * Get the set of all supported constant names.
     *
     * @return An unmodifiable set of constant names
     */
    public static Set<String> getConstantNames() {
        return Collections.unmodifiableSet(CONSTANT_MAPPINGS.keySet());
    }

    /**
     * Get the sorted set of all supported constant names.
     *
     * @return An unmodifiable set of constant names
     */
    public static Set<String> getSortedConstantNames() {
        List<String> sortedConstantNames = new ArrayList<>(getConstantNames());
        sortedConstantNames.sort((f1, f2) -> {
            String impl1 = getConstantImplementation(f1);
            String impl2 = getConstantImplementation(f2);
            return Integer.compare(impl2.length(), impl1.length());
        });

        return new LinkedHashSet<>(sortedConstantNames);
    }

    /**
     * Check if a constant name is supported.
     *
     * @param constantName The constant name to check
     * @return true if the constant is supported, false otherwise
     */
    public static boolean isConstant(String constantName) {
        return CONSTANT_MAPPINGS.containsKey(constantName.toLowerCase());
    }

    /**
     * Get the implementation method name for a given constant.
     *
     * @param constantName The constant name
     * @return The corresponding implementation method name
     * @throws IllegalArgumentException if the constant is not supported
     */
    public static String getConstantImplementation(String constantName) {
        String implementation = CONSTANT_MAPPINGS.get(constantName);
        if (implementation == null) {
            throw new IllegalArgumentException("Unsupported constant: " + constantName);
        }
        return implementation;
    }

}