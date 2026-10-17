package me.adda.terramath.math.formula;

import me.adda.terramath.api.TerrainSettingsManager;

import java.util.Random;

import static me.adda.terramath.math.formula.FormulaFit.num;

public class FormulaGenerator {
    private static final String FALLBACK = "perlin(x / 12, 0, z / 12) * 8";
    private static final int MAX_LENGTH = 170;

    private static final String[] WAVES = {
            "sin($u) * cos($v)", "sin($u + $v)", "(sin($u) + sin($v)) / 2", "sin($u + 2 * sin($v))",
            "abs(sin($u)) - abs(cos($v))", "tanh(2 * sin($u) + 2 * cos($v))", "sin(($u)^2 + ($v)^2)",
            "cos($u + sin($v * 1.7)) * sin($v)", "erf(2 * sin($u) + sin($v + $u))",
            "sin($u) * abs(cos($v))", "1 - 2 * abs(sin($u + $v))"
    };

    private static final String[] NOISES = {
            "perlin($u, 0, $v)", "simplex($u, $v)", "octaved($u, $v, 4, 0.5)", "octaved($u, $v, 2, 0.6)",
            "1 - 2 * abs(perlin($u, 0, $v))", "2 * abs(simplex($u, $v)) - 1", "tanh(3 * perlin($u, 0, $v))",
            "perlin($u + perlin($v, 3, $u) * 1.5, 0, $v)", "octaved($u, $v, 5, 0.45)"
    };

    private final Random rng;
    private final double cs;

    private FormulaGenerator(Random rng, double cs) {
        this.rng = rng;
        this.cs = cs;
    }

    public static String generateRandomFormula() {
        TerrainSettingsManager settings = TerrainSettingsManager.getInstance();
        FormulaGenerator gen = new FormulaGenerator(new Random(), Math.max(settings.getCoordinateScale(), 0.5));
        boolean equation = gen.rng.nextInt(4) == 0;
        for (int i = 0; i < 60; i++) {
            boolean useEquation = equation == (i < 40);
            String formula = useEquation
                    ? FormulaFit.equation(gen.volume(), gen.rng, settings)
                    : FormulaFit.heightmap(gen.surface(), gen.rng, settings);
            if (formula != null && formula.length() <= MAX_LENGTH) return formula;
        }
        return FALLBACK;
    }

    private double between(double lo, double hi) {
        return lo * Math.pow(hi / lo, rng.nextDouble());
    }

    private static String round(double v, int digits) {
        return num(Math.round(v * Math.pow(10, digits)) / Math.pow(10, digits));
    }

    static String wrap(String expr) {
        int open = expr.matches("[a-z0-9]+\\(.*") ? expr.indexOf('(') : expr.startsWith("(") ? 0 : -1;
        if (open >= 0) {
            int depth = 0;
            for (int i = open; i < expr.length(); i++) {
                if (expr.charAt(i) == '(') depth++;
                else if (expr.charAt(i) == ')' && --depth == 0) {
                    if (i == expr.length() - 1) return expr;
                    break;
                }
            }
        }
        return "(" + expr + ")";
    }

    private static String scaled(String coeff, String expr) {
        return coeff.equals("1") ? expr : coeff + " * " + expr;
    }

    private String waveDiv() {
        return round(between(70, 320) / (2 * Math.PI * cs), 1);
    }

    private String noiseDiv() {
        return round(between(70, 320) / cs, 0);
    }

    private String shifted(String var) {
        double offset = (rng.nextDouble() - 0.5) * 120 / cs;
        String amount = round(Math.abs(offset), 0);
        return amount.equals("0") ? var : "(" + var + (offset < 0 ? " - " : " + ") + amount + ")";
    }

    private String[] plane(boolean wave) {
        String d1 = wave ? waveDiv() : noiseDiv();
        String d2 = rng.nextInt(3) == 0 ? (wave ? waveDiv() : noiseDiv()) : d1;
        String[] uv;
        switch (rng.nextInt(5)) {
            case 0, 1 -> uv = new String[]{"x / " + d1, "z / " + d2};
            case 2, 3 -> {
                double angle = (0.35 + rng.nextDouble() * 0.85) * (rng.nextBoolean() ? 1 : -1);
                String c = round(Math.cos(angle), 2), s = round(Math.sin(angle), 2);
                uv = new String[]{"(x * " + c + " + z * " + s + ") / " + d1, "(z * " + c + " - x * " + s + ") / " + d2};
            }
            default -> {
                String ox = shifted("x"), oz = shifted("z");
                int arms = 1 + rng.nextInt(5);
                uv = new String[]{"sqrt(" + ox + "^2 + " + oz + "^2) / " + d1,
                        "atan2(" + oz + ", " + ox + ")" + (arms > 1 ? " * " + arms : "")};
            }
        }
        if (rng.nextInt(3) == 0) {
            uv[0] = "(" + uv[0] + " + " + scaled(round(0.3 + rng.nextDouble() * 1.7, 1), "sin(" + uv[1] + ")") + ")";
        }
        return uv;
    }

    private String layer() {
        boolean wave = rng.nextInt(5) < 2;
        String[] uv = plane(wave);
        String[] pool = wave ? WAVES : NOISES;
        return wrap(pool[rng.nextInt(pool.length)].replace("$u", uv[0]).replace("$v", uv[1]));
    }

    private String surface() {
        String acc = layer();
        int layers = rng.nextInt(5) == 0 ? 0 : 1;
        for (int i = 0; i < layers; i++) {
            String next = layer();
            String w = round(0.2 + rng.nextDouble() * 0.7, 1);
            acc = switch (rng.nextInt(5)) {
                case 0, 1 -> wrap(acc + " + " + scaled(w, next));
                case 2 -> "max(" + acc + ", " + next + ")";
                case 3 -> "min(" + acc + ", " + next + ")";
                default -> acc + " * (1 + " + scaled(w, next) + ")";
            };
        }
        return shape(acc);
    }

    private String shape(String g) {
        int k = 2 + rng.nextInt(3);
        return switch (rng.nextInt(10)) {
            case 0 -> "floor(" + g + " * " + k + ") * " + round(1.0 / k, 2);
            case 2 -> "tanh(" + g + " * " + round(1.5 + rng.nextDouble() * 2.5, 1) + ")";
            case 3 -> rng.nextBoolean() ? "cbrt(" + g + ")" : "(" + g + ")^3";
            case 4 -> "abs(" + g + ")";
            case 5 -> "clamp(" + g + " * " + round(1.5 + rng.nextDouble() * 1.5, 1) + ", -1, 1)";
            default -> g;
        };
    }

    private String volume() {
        String a = "x / " + waveDiv(), b = "y / " + waveDiv(), c = "z / " + waveDiv();
        String noise3 = "perlin(x / " + noiseDiv() + ", y / " + noiseDiv() + ", z / " + noiseDiv() + ")";
        if (rng.nextInt(3) == 0) {
            a = "(" + a + " + " + round(0.5 + rng.nextDouble(), 1) + " * " + noise3 + ")";
        }
        double y0 = between(30, 150) / cs;
        String ground = "(y - " + round(y0, 0) + ") / " + round(between(40, 200) / cs, 0);
        String radial = "sqrt(" + shifted("x") + "^2 + " + shifted("z") + "^2)";
        return switch (rng.nextInt(10)) {
            case 0 -> "sin(" + a + ") * cos(" + b + ") + sin(" + b + ") * cos(" + c + ") + sin(" + c + ") * cos(" + a + ")";
            case 1 -> "cos(" + a + ") + cos(" + b + ") + cos(" + c + ")";
            case 2 -> noise3 + " + " + ground;
            case 3 -> blob() + " + " + round(0.2 + rng.nextDouble() * 0.5, 1) + " * " + noise3;
            case 4 -> "min(" + blob() + ", " + blob() + ")";
            case 5 -> ground + " - " + round(0.5 + rng.nextDouble() * 1.5, 1) + " * " + surface() + " + "
                    + round(0.1 + rng.nextDouble() * 0.4, 1) + " * cos(" + b + ")";
            case 6 -> "(" + radial + " - " + round(between(30, 90) / cs, 0) + ")^2 + (y - " + round(y0, 0) + ")^2 * "
                    + round(0.5 + rng.nextDouble() * 2, 1);
            case 7 -> "abs(sin(" + a + ")) + abs(sin(" + c + ")) + " + round(0.3 + rng.nextDouble(), 1) + " * " + ground;
            case 8 -> radial + " / " + round(between(30, 100) / cs, 0) + " + " + round(0.3 + rng.nextDouble() * 0.7, 1)
                    + " * sin(" + b + " + " + round(0.5 + rng.nextDouble() * 2, 1) + " * " + noise3 + ")";
            default -> "cos(" + a + ") * cos(" + c + ") + ((y - " + round(y0, 0) + ") / " + round(between(30, 120) / cs, 0) + ")^2";
        };
    }

    private String blob() {
        double y0 = between(40, 140) / cs;
        return "sqrt((" + shifted("x") + " / " + round(between(40, 140) / cs, 0) + ")^2 + ((y - " + round(y0, 0) + ") / "
                + round(between(40, 140) / cs, 0) + ")^2 + (" + shifted("z") + " / " + round(between(40, 140) / cs, 0) + ")^2)";
    }
}
