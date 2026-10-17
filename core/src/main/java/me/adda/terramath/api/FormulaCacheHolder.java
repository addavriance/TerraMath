package me.adda.terramath.api;

import me.adda.terramath.math.parser.FormulaParser;

import java.util.regex.Pattern;

public class FormulaCacheHolder {
    private static final Pattern USES_Y = Pattern.compile("\\by\\b");

    public record Snapshot(String source, FormulaParser.CompiledFormula formula, boolean equation, boolean usesY) {}

    private static volatile Snapshot snapshot = null;

    public static Snapshot snapshot() {
        String current = TerrainFormulaManager.getInstance().getFormula();
        Snapshot cached = snapshot;
        if (cached != null && cached.source().equals(current)) return cached;
        if (current.trim().isEmpty()) return null;

        synchronized (FormulaCacheHolder.class) {
            cached = snapshot;
            if (cached != null && cached.source().equals(current)) return cached;
            cached = new Snapshot(current, FormulaParser.parse(current), current.contains("="), USES_Y.matcher(current).find());
            snapshot = cached;
            return cached;
        }
    }

    public static FormulaParser.CompiledFormula getParsedFormula() {
        Snapshot current = snapshot();
        return current == null ? null : current.formula();
    }

    public static boolean isEquationMode() {
        Snapshot current = snapshot;
        return current != null && current.equation();
    }

    public static void resetCache() {
        snapshot = null;
    }
}
