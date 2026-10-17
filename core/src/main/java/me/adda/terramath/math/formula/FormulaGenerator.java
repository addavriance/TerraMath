package me.adda.terramath.math.formula;

import me.adda.terramath.api.TerrainSettingsManager;
import me.adda.terramath.math.formula.FormulaFit.Profile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import static me.adda.terramath.math.formula.FormulaFit.num;

public class FormulaGenerator {
    private static final String FALLBACK = "perlin(x / 12, 0, z / 12) * 8";
    private static final int MAX_LENGTH = 220;
    private static final int RECENT = 5;
    private static final int WILD_PERCENT = 30;
    private static final int WILD_LENGTH = 360;
    private static final int PERLIN_FREQUENCY = 16;

    private static final Deque<String> recent = new ArrayDeque<>();

    private record Kind(String name, int weight, Function<FormulaGenerator, String> make) {}

    private static final List<Kind> HEIGHT = List.of(
            new Kind("islands", 10, g -> g.islands()),
            new Kind("canyons", 10, g -> g.canyons()),
            new Kind("mesas", 10, g -> g.mesas()),
            new Kind("spires", 8, g -> g.spires()),
            new Kind("craters", 9, g -> g.craters()),
            new Kind("dunes", 8, g -> g.dunes()),
            new Kind("rivers", 9, g -> g.rivers()),
            new Kind("cliffs", 9, g -> g.cliffs()),
            new Kind("domes", 8, g -> g.domes()),
            new Kind("mosaic", 6, g -> g.mosaic()),
            new Kind("ridges", 9, g -> g.ridges()),
            new Kind("moire", 4, g -> g.moire()),
            new Kind("spiral", 2, g -> g.spiral())
    );

    private static final List<Kind> VOLUME = List.of(
            new Kind("floating", 7, g -> g.floating()),
            new Kind("caves", 8, g -> g.caves()),
            new Kind("overhangs", 6, g -> g.overhangs()),
            new Kind("columns", 5, g -> g.columns()),
            new Kind("torus", 3, g -> g.torus()),
            new Kind("vortex", 2, g -> g.vortex()),
            new Kind("sponge", 2, g -> g.sponge())
    );

    private final Random rng;
    private final double cs;
    private double fillMin = 0.15, fillMax = 0.4;
    private Profile profile = Profile.of(30, 70, 90, 150);

    private FormulaGenerator(Random rng, double cs) {
        this.rng = rng;
        this.cs = cs;
    }

    public record Generated(String formula, String kind) {}

    public static String generateRandomFormula() {
        return generate().formula();
    }

    public static Generated generate() {
        TerrainSettingsManager settings = TerrainSettingsManager.getInstance();
        FormulaGenerator gen = new FormulaGenerator(new Random(), Math.max(settings.getCoordinateScale(), 0.5));
        boolean volume = gen.rng.nextInt(4) == 0;
        if (gen.rng.nextInt(100) < WILD_PERCENT) {
            for (int i = 0; i < 12; i++) {
                String wild = volume ? gen.wildVolume(settings) : gen.wildHeight(settings);
                if (wild != null && wild.length() <= WILD_LENGTH) return new Generated(wild, "wild");
            }
        }
        for (int round = 0; round < 6; round++) {
            Kind kind = gen.pick(volume ? VOLUME : HEIGHT, round < 3);
            String formula = gen.fit(kind, volume, settings);
            if (formula != null) {
                remember(kind.name());
                return new Generated(formula, kind.name());
            }
        }
        return new Generated(FALLBACK, "fallback");
    }

    private String fit(Kind kind, boolean volume, TerrainSettingsManager settings) {
        for (int attempt = 0; attempt < 30; attempt++) {
            String expr = kind.make().apply(this);
            String formula = volume
                    ? FormulaFit.equation(expr, fillMin, fillMax, rng, settings)
                    : FormulaFit.heightmap(expr, profile, rng, settings);
            if (formula != null && formula.length() <= MAX_LENGTH) return formula;
        }
        return null;
    }

    private String wildHeight(TerrainSettingsManager settings) {
        int mode = rng.nextInt(10);
        if (mode < 4) {
            Kind kind = pick(HEIGHT, false);
            for (int attempt = 0; attempt < 20; attempt++) {
                String warped = warp(kind.make().apply(this));
                String formula = warped == null ? null : FormulaFit.heightmap(warped, profile, rng, settings);
                if (formula != null) return formula;
            }
            return null;
        }
        String a = fit(pick(HEIGHT, false), false, settings);
        String b = fit(pick(HEIGHT, false), false, settings);
        if (a == null || b == null || a.equals(b)) return null;
        String formula;
        if (mode < 7) {
            String mask = "clamp(" + round(1.5 + rng.nextDouble() * 2, 1) + " * simplex(x / " + noiseDiv(100, 300) + ", z / " + noiseDiv(100, 300) + ") + 0.5, 0, 1)";
            formula = "(" + a + ") * " + mask + " + (" + b + ") * (1 - " + mask + ")";
        } else {
            formula = (mode < 9 ? "max" : "min") + "(" + a + ", " + b + ")";
        }
        return FormulaFit.checkHeights(formula, 0.65, settings) ? formula : null;
    }

    private String wildVolume(TerrainSettingsManager settings) {
        String terrain = fit(pick(HEIGHT, false), false, settings);
        String shape = fit(pick(VOLUME, false), true, settings);
        if (terrain == null || shape == null) return null;
        int split = shape.lastIndexOf(" = ");
        String lhs = shape.substring(0, split), c = shape.substring(split + 3);

        boolean density = settings.isUseDensityMode();
        double cs = Math.max(settings.getCoordinateScale(), 0.5);
        String ground = "y - (" + round((density ? settings.getBaseHeight() : 64.0) / cs, 1) + " + "
                + (density ? round(settings.getHeightVariation(), 2) + " * (" + terrain + ")" : terrain) + ")";
        String inside = c.startsWith("-") ? "(" + c + ")" : c;
        String formula = rng.nextBoolean()
                ? "min(" + ground + ", " + lhs + " - " + inside + ") = " + round(density ? (settings.getBaseHeight() - 64.0) / cs : 0, 2)
                : "max(" + ground + ", " + inside + " - (" + lhs + ")) = " + round(density ? (settings.getBaseHeight() - 64.0) / cs : 0, 2);
        return FormulaFit.checkVolume(formula, settings) ? formula : null;
    }

    private String warp(String raw) {
        String marked = raw.replaceAll("\\bx\\b", "\u0001").replaceAll("\\bz\\b", "\u0002");
        long uses = marked.chars().filter(ch -> ch == 1 || ch == 2).count();
        if (uses == 0 || uses > 8) return null;

        String amount = round(between(20, 90) / cs, 0);
        String x, z;
        switch (rng.nextInt(4)) {
            case 0 -> {
                String d = noiseDiv(60, 220);
                x = "(x + " + amount + " * simplex(z / " + d + ", x / " + d + "))";
                z = "(z + " + amount + " * simplex(x / " + d + " + 3, z / " + d + "))";
            }
            case 1 -> {
                x = "(x + " + amount + " * sin(z / " + waveDiv(80, 250) + "))";
                z = "z";
            }
            case 2 -> {
                x = "abs(" + at("x", 60) + ")";
                z = rng.nextBoolean() ? "abs(" + at("z", 60) + ")" : "z";
            }
            default -> {
                x = "(x + " + amount + " * sin(z / " + waveDiv(80, 250) + "))";
                z = "(z + " + amount + " * sin(x / " + waveDiv(80, 250) + "))";
            }
        }
        return marked.replace("\u0001", x).replace("\u0002", z);
    }

    private static synchronized void remember(String name) {
        recent.addLast(name);
        while (recent.size() > RECENT) recent.removeFirst();
    }

    private Kind pick(List<Kind> kinds, boolean avoidRecent) {
        List<Kind> pool = new ArrayList<>();
        synchronized (FormulaGenerator.class) {
            for (Kind k : kinds) if (!avoidRecent || !recent.contains(k.name())) pool.add(k);
        }
        if (pool.isEmpty()) pool = kinds;
        int roll = rng.nextInt(pool.stream().mapToInt(Kind::weight).sum());
        for (Kind k : pool) {
            roll -= k.weight();
            if (roll < 0) return k;
        }
        return pool.get(0);
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

    private static String nest(String fn, List<String> args) {
        String result = args.get(args.size() - 1);
        for (int i = args.size() - 2; i >= 0; i--) result = fn + "(" + args.get(i) + ", " + result + ")";
        return result;
    }

    private double between(double lo, double hi) {
        return lo * Math.pow(hi / lo, rng.nextDouble());
    }

    private static String round(double v, int digits) {
        return num(Math.round(v * Math.pow(10, digits)) / Math.pow(10, digits));
    }

    private String noiseDiv(double minBlocks, double maxBlocks) {
        return round(between(minBlocks, maxBlocks) / cs, 0);
    }

    private String perlinDiv(double minBlocks, double maxBlocks) {
        return round(between(minBlocks, maxBlocks) * PERLIN_FREQUENCY / cs, 0);
    }

    private String waveDiv(double minBlocks, double maxBlocks) {
        return round(between(minBlocks, maxBlocks) / (2 * Math.PI * cs), 1);
    }

    private String field(double minBlocks, double maxBlocks) {
        String d = perlinDiv(minBlocks, maxBlocks);
        return switch (rng.nextInt(4)) {
            case 0 -> "simplex(x / " + noiseDiv(minBlocks, maxBlocks) + ", z / " + noiseDiv(minBlocks, maxBlocks) + ")";
            case 1 -> "perlin(x / " + d + ", 0, z / " + d + ")";
            default -> "octaved(x / " + d + ", z / " + d + ", " + (2 + rng.nextInt(3)) + ", 0.5)";
        };
    }

    private String warped(double minBlocks, double maxBlocks) {
        String d = perlinDiv(minBlocks, maxBlocks);
        if (rng.nextBoolean()) return field(minBlocks, maxBlocks);
        return "octaved(x / " + d + " + " + round((0.5 + rng.nextDouble() * 1.5) / PERLIN_FREQUENCY, 2) + " * sin(z / " + waveDiv(60, 200)
                + "), z / " + d + ", 3, 0.5)";
    }

    private String at(String var, double spanBlocks) {
        long offset = Math.round((rng.nextDouble() - 0.5) * spanBlocks / cs);
        return offset == 0 ? var : "(" + var + (offset < 0 ? " - " : " + ") + Math.abs(offset) + ")";
    }

    private String dist2(double spanBlocks) {
        return at("x", spanBlocks) + "^2 + " + at("z", spanBlocks) + "^2";
    }

    private String[] rotated() {
        double angle = rng.nextDouble() * Math.PI;
        String c = round(Math.cos(angle), 2), s = round(Math.sin(angle), 2);
        return new String[]{"(x * " + c + " + z * " + s + ")", "(z * " + c + " - x * " + s + ")"};
    }

    private void heights(double lowMin, double lowMax, double highMin, double highMax) {
        profile = Profile.of(lowMin, lowMax, highMin, highMax);
    }

    private String islands() {
        heights(20, 50, 90, 150);
        profile = profile.spread(0, 0.7);
        int n = 3 + rng.nextInt(2);
        List<String> bumps = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double radius = between(40, 110) / cs;
            bumps.add("exp(-(" + dist2(170) + ") / " + round(radius * radius, 0) + ")");
        }
        return nest("max", bumps) + " + 0.2 * " + field(40, 120);
    }

    private String canyons() {
        heights(20, 60, 100, 170);
        profile = profile.spread(0.02, 0.55);
        return "sqrt(abs(" + warped(70, 250) + "))";
    }

    private String mesas() {
        heights(30, 60, 90, 150);
        profile = profile.spread(0.05, 0.55);
        int steps = 3 + rng.nextInt(4);
        return "floor(" + warped(100, 300) + " * " + steps + ") * " + round(1.0 / steps, 2) + " + 0.15 * " + field(60, 200);
    }

    private String spires() {
        heights(40, 70, 110, 190);
        profile = profile.spread(0.02, 0.6);
        return "exp(" + round(2 + rng.nextDouble() * 1.5, 1) + " * " + field(70, 200) + ")";
    }

    private String craters() {
        heights(60, 100, 100, 160);
        profile = profile.spread(0.03, 0.55);
        StringBuilder sb = new StringBuilder(round(0.3 + rng.nextDouble() * 0.4, 1) + " * " + field(60, 200));
        for (int i = 0; i < 3; i++) {
            double radius = between(25, 60) / cs;
            sb.append(" - ").append(round(0.6 + rng.nextDouble() * 0.6, 1)).append(" * exp(-(").append(dist2(160))
                    .append(") / ").append(round(radius * radius, 0)).append(")");
        }
        return sb.toString();
    }

    private String dunes() {
        double low = 55 + rng.nextDouble() * 25;
        profile = Profile.of(low, low + 5, low + 30, low + 65).spread(0.05, 0.7);
        String[] pq = rotated();
        return "(1 - abs(sin(" + pq[0] + " / " + waveDiv(60, 200) + " + " + round(0.5 + rng.nextDouble() * 1.2, 1) + " * sin("
                + pq[1] + " / " + waveDiv(120, 400) + "))))^2 * (0.6 + 0.4 * " + field(80, 250) + ")";
    }

    private String rivers() {
        heights(52, 62, 85, 150);
        profile = profile.spread(0.0, 0.6);
        String[] pq = rotated();
        double width = between(8, 30) / cs;
        String bed = "tanh(abs(" + pq[1] + " - " + round(between(15, 50) / cs, 0) + " * sin(" + pq[0] + " / " + waveDiv(150, 450)
                + ")) / " + round(width, 1) + ")";
        String second = rng.nextBoolean() ? "" : ", tanh(abs(" + pq[0] + " + " + round(between(10, 40) / cs, 0) + " * sin(" + pq[1]
                + " / " + waveDiv(150, 450) + ")) / " + round(width * 1.4, 1) + ")";
        String channels = second.isEmpty() ? bed : "min(" + bed + second + ")";
        return channels + " + 0.3 * " + field(40, 140);
    }

    private String cliffs() {
        heights(35, 70, 100, 150);
        profile = profile.spread(0.05, 0.7);
        return "tanh(" + round(3 + rng.nextDouble() * 4, 1) + " * " + warped(80, 260) + ") + 0.15 * " + field(30, 90);
    }

    private String domes() {
        heights(20, 50, 90, 150);
        profile = profile.spread(0, 0.7);
        int n = 4 + rng.nextInt(2);
        List<String> cones = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double radius = between(50, 130) / cs;
            cones.add("1 - (" + dist2(190) + ") / " + round(radius * radius, 0));
        }
        return nest("max", cones);
    }

    private String mosaic() {
        heights(30, 60, 90, 150);
        profile = profile.spread(0.03, 0.5);
        String cell = round(between(10, 40) / cs, 0);
        String k = round(0.25 + rng.nextDouble() * 0.65, 2);
        return "perlin(floor(x / " + cell + ") * " + k + ", 0, floor(z / " + cell + ") * " + k + ")";
    }

    private String ridges() {
        heights(30, 60, 110, 190);
        profile = profile.spread(0.03, 0.55);
        String d = perlinDiv(100, 350);
        String warp = rng.nextBoolean() ? "" : " + " + round((0.5 + rng.nextDouble() * 1.2) / PERLIN_FREQUENCY, 2) + " * simplex(z / " + noiseDiv(60, 180) + ", x / " + noiseDiv(60, 180) + ")";
        return "1 - abs(2 * perlin(x / " + d + warp + ", 0, z / " + d + "))";
    }

    private String moire() {
        heights(45, 75, 95, 140);
        String expr = "";
        int n = 2 + rng.nextInt(2);
        for (int i = 0; i < n; i++) {
            String[] pq = rotated();
            expr += (i > 0 ? " + " : "") + (i == 2 ? "0.5 * " : "") + "sin(" + pq[0] + " / " + waveDiv(80, 300) + ")";
        }
        return expr;
    }

    private String spiral() {
        heights(45, 75, 95, 140);
        String ox = at("x", 80), oz = at("z", 80);
        return "sin(atan2(" + oz + ", " + ox + ") * " + (2 + rng.nextInt(3)) + " + sqrt(" + ox + "^2 + " + oz + "^2) / " + waveDiv(60, 160) + ")";
    }

    private String perlin3(double min, double max) {
        return "perlin(x / " + perlinDiv(min, max) + ", y / " + perlinDiv(min, max) + ", z / " + perlinDiv(min, max) + ")";
    }

    private String ground(double heightBlocks) {
        return "(y - " + round(heightBlocks / cs, 0) + ") / " + round(between(40, 200) / cs, 0);
    }

    private String blob() {
        return "sqrt((" + at("x", 160) + " / " + round(between(40, 120) / cs, 0) + ")^2 + ((y - " + round(between(60, 160) / cs, 0)
                + ") / " + round(between(30, 90) / cs, 0) + ")^2 + (" + at("z", 160) + " / " + round(between(40, 120) / cs, 0) + ")^2)";
    }

    private String floating() {
        fillMin = 0.1; fillMax = 0.28;
        return "min(" + blob() + ", " + blob() + ") + " + round(0.2 + rng.nextDouble() * 0.5, 1) + " * " + perlin3(40, 120);
    }

    private String caves() {
        fillMin = 0.3; fillMax = 0.5;
        return ground(between(60, 130)) + " - " + round(0.5 + rng.nextDouble() * 1.2, 1) + " * " + field(100, 300)
                + " + " + round(6 + rng.nextDouble() * 6, 0) + " * max(0, " + round(0.08 + rng.nextDouble() * 0.1, 2) + " - abs(" + perlin3(40, 120) + "))";
    }

    private String overhangs() {
        fillMin = 0.25; fillMax = 0.5;
        return round(2 + rng.nextDouble() * 4, 0) + " * " + perlin3(70, 200) + " + " + ground(between(60, 130));
    }

    private String columns() {
        fillMin = 0.12; fillMax = 0.3;
        List<String> shafts = new ArrayList<>();
        int n = 2 + rng.nextInt(2);
        for (int i = 0; i < n; i++) {
            String r = round(between(15, 50) / cs, 0);
            shafts.add("sqrt((" + at("x", 160) + " / " + r + ")^2 + (" + at("z", 160) + " / " + r + ")^2)");
        }
        return nest("min", shafts) + " + 0.3 * " + perlin3(40, 120);
    }

    private String torus() {
        fillMin = 0.1; fillMax = 0.25;
        return "(sqrt(" + dist2(100) + ") - " + round(between(30, 90) / cs, 0) + ")^2 + (y - " + round(between(60, 140) / cs, 0)
                + ")^2 * " + round(0.5 + rng.nextDouble() * 2, 1);
    }

    private String vortex() {
        fillMin = 0.12; fillMax = 0.3;
        return "sqrt(" + dist2(100) + ") / " + round(between(30, 100) / cs, 0) + " + " + round(0.3 + rng.nextDouble() * 0.7, 1)
                + " * sin(y / " + waveDiv(40, 120) + " + " + round(0.5 + rng.nextDouble() * 2, 1) + " * " + perlin3(50, 150) + ")";
    }

    private String sponge() {
        fillMin = 0.15; fillMax = 0.35;
        String a = "x / " + waveDiv(60, 200), b = "y / " + waveDiv(60, 200), c = "z / " + waveDiv(60, 200);
        return "sin(" + a + ") * cos(" + b + ") + sin(" + b + ") * cos(" + c + ") + sin(" + c + ") * cos(" + a + ")";
    }
}
