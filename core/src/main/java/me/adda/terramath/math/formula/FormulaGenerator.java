package me.adda.terramath.math.formula;

import me.adda.terramath.math.functions.MathFunctionsRegistry;
import me.adda.terramath.math.constants.MathConstantsRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class FormulaGenerator {
    private static final String[] TERRAIN_TEMPLATES = {

        // Simple wave
        "sin(x) * {A}",
        "sin(x) * {A} + cos(z) * {B}",
        "sin(x) * cos(z) * {A}",
        "sin(x + z) * {A}",
        "sin(x / {S}) * {A} + cos(z / {S}) * {B}",
        "sin(x / {S}) * sin(z / {S}) * {A}",
        "sin(x / {S}) * {A} - cos(z / {S}) * {B}",
        "cos(x / {S}) * sin(z / {S}) * {A} + sin(x / {S}) * {B}",

        // Multi-octave manual FBM
        "sin(x / {S}) * {A} + sin(x / {S2}) * {B} + cos(z / {S}) * {B}",
        "sin(x / {S}) * {A} + sin(2*x / {S}) * {B} + sin(4*x / {S}) * {C}",
        "cos(x / {S}) * {A} + cos(2*x / {S}) * {B} + cos(z / {S}) * {C}",
        "sin(x / {S}) * {A} + sin(x / {S2}) * {B} + sin(z / {S}) * {A} + sin(z / {S2}) * {B}",
        "sin(x / {S}) * {A} + cos(z / {S2}) * {B} + sin((x + z) / {S3}) * {C}",
        "sin(x / {S}) * {A} + sin(z / {S}) * {A} + sin((x + z) / {S2}) * {B}",

        // Ridged fractal (abs of sin = ridges / mountains)
        "abs(sin(x / {S})) * {A}",
        "{A} - abs(sin(x / {S}) * {A})",
        "abs(sin(x / {S})) * {A} - abs(cos(z / {S})) * {B}",
        "abs(sin(x / {S}) + cos(z / {S})) * {A}",
        "abs(sin(x / {S}) * cos(z / {S})) * {A}",
        "{A} - abs(sin(x / {S})) * {A} - abs(cos(z / {S2})) * {B}",
        "-abs(sin(x / {S}) * {A} + cos(z / {S}) * {B})",
        "abs(sin(x / {S}) - cos(z / {S})) * {A}",
        "({A} - abs(sin(x / {S}) * {A})) + ({B} - abs(cos(z / {S}) * {B}))",

        // Domain warping (input of outer sin is warped by inner function)
        "sin(x / {S} + sin(z / {S2}) * {A2}) * {A}",
        "sin(x / {S} + cos(z / {S2}) * {A2}) * {A} + cos(z / {S}) * {B}",
        "sin(x / {S} + sin(x / {S2}) * {A2}) * {A}",
        "cos((x + sin(z / {S2}) * {A2}) / {S}) * {A}",
        "sin(x / {S} + perlin(x / {S2}, 0, z / {S2}) * {A2}) * {A}",

        // Noise-based terrain
        "perlin(x / {S}, 0, z / {S}) * {A}",
        "simplex(x / {S}, z / {S}) * {A}",
        "octaved(x / {S}, z / {S}, 4, 0.5) * {A}",
        "octaved(x / {S}, z / {S}, 6, 0.5) * {A}",
        "octaved(x / {S2}, z / {S2}, 3, 0.6) * {A} + sin(x / {S}) * {B}",
        "perlin(x / {S}, 0, z / {S}) * {A} + sin(x / {S2}) * {B}",
        "simplex(x / {S}, z / {S}) * {A} + cos(z / {S2}) * {B}",
        "{A} - abs(octaved(x / {S}, z / {S}, 4, 0.5)) * {A}",
        "octaved(x / {S}, z / {S}, 5, 0.5) * {A} + octaved(x / {S2}, z / {S2}, 3, 0.5) * {B}",

        // Terraced / stepped
        "floor(sin(x / {S}) * {A2}) * {B}",
        "floor(perlin(x / {S}, 0, z / {S}) * {A2}) * {B}",
        "floor(octaved(x / {S}, z / {S}, 3, 0.5) * {A2}) * {B}",
        "floor(abs(sin(x / {S})) * {A2}) * {B}",

        // Radial / circular
        "sin(sqrt(x^2 + z^2) / {S}) * {A}",
        "abs(sin(sqrt(x^2 + z^2) / {S})) * {A}",
        "{A} - abs(sin(sqrt(x^2 + z^2) / {S}) * {A})",
        "cos(sqrt(x^2 + z^2) / {S}) * {A} + sin(x / {S2}) * {B}",
        "octaved(sqrt(x^2 + z^2) / {S}, 0, 4, 0.5) * {A}",

        // Spiral
        "sin(atan2(z, x) * {A2} + sqrt(x^2 + z^2) / {S}) * {A}",
        "cos(atan2(z, x) * {A2} + sqrt(x^2 + z^2) / {S2}) * {A} + sin(sqrt(x^2 + z^2) / {S}) * {B}",

        // Hyperbolic / tanh
        "tanh(sin(x / {S}) * {A}) * {B}",
        "tanh(x / {S}) * {A} + tanh(z / {S}) * {B}",
        "tanh(sqrt(x^2 + z^2) / {S}) * {A}",
        "tanh(sin(x / {S}) + cos(z / {S})) * {A}",

        // Abs and sign patterns
        "abs(cos(x / {S}) - sin(z / {S})) * {A}",
        "(abs(sin(x / {S})) - abs(cos(z / {S}))) * {A}",
        "sin(x / {S}) * {A} * sign(cos(z / {S}))",
        "sin(x * cos(z / {S})) * {A}",
    };

    private static final String[] EQUATION_TEMPLATES = {
        "x^2 + z^2 = {N}",
        "x^2 + y^2 + z^2 = {N2}",
        "x^2 - z^2 = {N3}",
        "abs(x) + abs(z) = {N3}",
        "abs(x) + abs(y) + abs(z) = {N3}",

        "x^2 + z^2 = y * {A}",
        "x^2 / {N3} - z^2 / {N3} = 1",
        "x * z = y * {A}",
        "x^2 + z^2 = y^2",
        "abs(x) + abs(z) = abs(y) + {N3}",


        "sin(x / {S}) * {A} = z",
        "sin(x / {S}) * {A} = y",
        "sin(x / {S}) * {A} + cos(z / {S}) * {B} = y",
        "sin(x / {S}) * cos(z / {S}) * {A} = y",
        "octaved(x / {S}, z / {S}, 4, 0.5) * {A} = y",
        "abs(sin(x / {S})) * {A} + abs(cos(z / {S})) * {B} = y",

        "(sqrt(x^2 + z^2) - {R})^2 + y^2 = {r}",

        "sqrt(x^2 + z^2) / {S} = sin(y / {S})",
        "sqrt(x^2 + z^2) = {A} * abs(sin(y / {S}))",

        // Schwartz P
        "cos(x / {S}) + cos(y / {S}) + cos(z / {S}) = 0",
        // Gyroid
        "sin(x / {S}) * cos(y / {S}) + sin(y / {S}) * cos(z / {S}) + sin(z / {S}) * cos(x / {S}) = 0",
        // Diamond (Schwartz D)
        "sin(x / {S}) * sin(y / {S}) * sin(z / {S}) + sin(x / {S}) * cos(y / {S}) * cos(z / {S}) + cos(x / {S}) * sin(y / {S}) * cos(z / {S}) + cos(x / {S}) * cos(y / {S}) * sin(z / {S}) = 0",
        // Neovius
        "3 * (cos(x / {S}) + cos(y / {S}) + cos(z / {S})) + 4 * cos(x / {S}) * cos(y / {S}) * cos(z / {S}) = 0",

        "sin(x / {S}) + cos(z / {S}) = sin(y / {S})",
        "sin(x / {S}) * sin(y / {S}) + sin(y / {S}) * sin(z / {S}) = 0",
        "abs(sin(x / {S})) + abs(cos(z / {S})) = y / {A} + 1",
        "octaved(x / {S}, z / {S}, 3, 0.5) * {A} + octaved(y / {S}, z / {S}, 3, 0.5) * {A} = 0",
    };

    private static final Set<String> NOISE_FN_NAMES =
            Set.of("perlin", "simplex", "normal", "blended", "octaved");

    private static final List<String> FNS_1ARG;
    private static final List<String> FNS_2ARG;
    private static final List<String> NOISE_FNS;
    private static final List<String> CONST_NAMES;

    static {
        FNS_1ARG    = new ArrayList<>();
        FNS_2ARG    = new ArrayList<>();
        NOISE_FNS   = new ArrayList<>();
        CONST_NAMES = new ArrayList<>();

        for (String c : MathConstantsRegistry.getConstantNames()) {
            if (c.matches("[a-z]+")) CONST_NAMES.add(c); // skip Unicode aliases (π, φ …)
        }
        for (String fn : MathFunctionsRegistry.getFunctionNames()) {
            if (NOISE_FN_NAMES.contains(fn)) { NOISE_FNS.add(fn); continue; }
            int arity = MathFunctionsRegistry.getArity(fn);
            if (arity == 1) FNS_1ARG.add(fn);
            else if (arity == 2) FNS_2ARG.add(fn);
            // 0-arg (rand), 3-arg (clamp), unknowns skipped intentionally for random gen
        }
    }

    public static String generateRandomFormula() {
        Random rng = new Random();
        int roll = rng.nextInt(4);
        if (roll == 0) return generateEquation(rng);
        if (roll == 1) return generatePureRandom(rng);
        return generateTerrainFunction(rng);
    }

    private static String generatePureRandom(Random rng) {
        int terms = 1 + rng.nextInt(3);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < terms; i++) {
            if (i > 0) sb.append(rng.nextBoolean() ? " + " : " - ");
            int amp = 5 + rng.nextInt(20);
            sb.append(amp).append(" * ").append(randomBounded(rng, 2));
        }
        return sb.toString();
    }

    private static String randomBounded(Random rng, int depth) {
        int pick = rng.nextInt(depth <= 0 ? 3 : 10);
        switch (pick) {
            case 0, 1, 2 -> {
                String fn = FNS_1ARG.get(rng.nextInt(FNS_1ARG.size()));
                return fn + "(" + randomArg(rng, depth - 1) + ")";
            }
            // random 2-arg function from registry
            case 3 -> {
                String fn = FNS_2ARG.get(rng.nextInt(FNS_2ARG.size()));
                return fn + "(" + randomArg(rng, depth - 1)
                         + ", " + randomArg(rng, depth - 1) + ")";
            }
            // noise from registry
            case 4 -> { return randomNoiseExpr(rng); }
            // composition fn1(fn2(arg))
            case 5 -> {
                String f1 = FNS_1ARG.get(rng.nextInt(FNS_1ARG.size()));
                String f2 = FNS_1ARG.get(rng.nextInt(FNS_1ARG.size()));
                return f1 + "(" + f2 + "(" + simpleArg(rng) + "))";
            }
            // Gaussian bell exp(-r²/N)
            case 6 -> {
                int n = 500 + rng.nextInt(2000);
                String r2 = rng.nextBoolean() ? "(x^2 + z^2)" : (rng.nextBoolean() ? "x^2" : "z^2");
                return "exp(-" + r2 + " / " + n + ")";
            }
            // fn with constant as multiplier in arg
            case 7 -> {
                String fn = FNS_1ARG.get(rng.nextInt(FNS_1ARG.size()));
                String c  = CONST_NAMES.get(rng.nextInt(CONST_NAMES.size()));
                int s = 1 + rng.nextInt(8);
                String v = rng.nextBoolean() ? "x" : "z";
                return fn + "(" + v + " * " + c + " / " + (s * 10) + ")";
            }
            // min/max of two bounded
            case 8 -> {
                String op = rng.nextBoolean() ? "min" : "max";
                return op + "(" + randomBounded(rng, depth - 1)
                         + ", " + randomBounded(rng, depth - 1) + ")";
            }
            // clamp(expr, -1, 1)
            case 9 -> {
                String fn = FNS_1ARG.get(rng.nextInt(FNS_1ARG.size()));
                return "clamp(" + fn + "(" + randomArg(rng, depth - 1) + "), -1, 1)";
            }
            default -> { return "sin(" + simpleArg(rng) + ")"; }
        }
    }

    private static String randomArg(Random rng, int depth) {
        if (depth <= 0) return simpleArg(rng);
        int s  = 2 + rng.nextInt(9);
        int s2 = 2 + rng.nextInt(9);
        int k  = 1 + rng.nextInt(4);
        String v1 = rng.nextBoolean() ? "x" : "z";
        String v2 = v1.equals("x") ? "z" : "x";
        return switch (rng.nextInt(9)) {
            case 0 -> simpleArg(rng);
            case 1 -> "(" + v1 + " + " + v2 + ") / " + s;
            case 2 -> "(" + v1 + " - " + v2 + ") / " + s;
            case 3 -> v1 + " / " + s + " + " + v2 + " / " + s2;
            case 4 -> "sqrt(x^2 + z^2) / " + s;
            case 5 -> "atan2(" + v1 + ", " + v2 + ")";
            case 6 -> v1 + " / " + s + " + sin(" + v2 + " / " + s2 + ") * " + k;
            case 7 -> v1 + " / " + s + " + cos(" + v1 + " / " + s2 + ") * " + k;
            case 8 -> "atan2(" + v1 + ", " + v2 + ") * " + k
                      + " + sqrt(x^2 + z^2) / " + s;
            default -> simpleArg(rng);
        };
    }


    private static String randomNoiseExpr(Random rng) {
        String fn = NOISE_FNS.get(rng.nextInt(NOISE_FNS.size()));
        int s = 2 + rng.nextInt(8);
        return switch (fn) {
            case "simplex"  -> "simplex(x / " + s + ", z / " + s + ")";
            case "octaved"  -> "octaved(x / " + s + ", z / " + s
                               + ", " + (2 + rng.nextInt(4)) + ", 0.5)";
            default         -> fn + "(x / " + s + ", 0, z / " + s + ")";
        };
    }

    private static String simpleArg(Random rng) {
        int s = 2 + rng.nextInt(9);
        return (rng.nextBoolean() ? "x" : "z") + " / " + s;
    }

    private static String generateTerrainFunction(Random rng) {
        String template = TERRAIN_TEMPLATES[rng.nextInt(TERRAIN_TEMPLATES.length)];
        return fillParams(template, rng);
    }

    private static String generateEquation(Random rng) {
        String template = EQUATION_TEMPLATES[rng.nextInt(EQUATION_TEMPLATES.length)];
        return fillParams(template, rng);
    }

    private static String fillParams(String template, Random rng) {
        String result = template;

        // Amplitudes: [5, 24]
        result = result.replace("{A}",  String.valueOf(5 + rng.nextInt(20)));
        result = result.replace("{B}",  String.valueOf(5 + rng.nextInt(20)));
        result = result.replace("{C}",  String.valueOf(5 + rng.nextInt(20)));

        // Small integer [2, 5] - spiral arms, terrace steps, warp magnitude
        result = result.replace("{A2}", String.valueOf(2 + rng.nextInt(4)));

        // Primary scale [2, 8]
        result = result.replace("{S}",  String.valueOf(2 + rng.nextInt(7)));

        // Secondary scale [4, 12]
        result = result.replace("{S2}", String.valueOf(4 + rng.nextInt(9)));

        // Tertiary scale [6, 18]
        result = result.replace("{S3}", String.valueOf(6 + rng.nextInt(13)));

        // Cylinder / diamond N: [500, 3000]
        result = result.replace("{N}",  String.valueOf(500 + rng.nextInt(2501)));

        // Sphere N: [500, 2000]
        result = result.replace("{N2}", String.valueOf(500 + rng.nextInt(1501)));

        // Small N (hyperbolic, diamond): [200, 800]
        result = result.replace("{N3}", String.valueOf(200 + rng.nextInt(601)));

        // Torus major radius: [20, 35]
        result = result.replace("{R}",  String.valueOf(20 + rng.nextInt(16)));

        // Torus tube radius: [5, 12]
        result = result.replace("{r}",  String.valueOf(5 + rng.nextInt(8)));

        return result;
    }
}
