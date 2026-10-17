package me.adda.terramath.api;

import me.adda.terramath.math.parser.FormulaParser;

public class FormulaCacheHolder {
    private static FormulaParser.CompiledFormula parsedFormula = null;
    private static String lastFormula = null;
    private static boolean equationMode = false;

    public static FormulaParser.CompiledFormula getParsedFormula() {
        String currentFormula = TerrainFormulaManager.getInstance().getFormula();
        if ((parsedFormula == null || !currentFormula.equals(lastFormula)) && !currentFormula.trim().isEmpty()) {
            equationMode = currentFormula.contains("=");
            parsedFormula = FormulaParser.parse(currentFormula);
            lastFormula = currentFormula;
        }
        return parsedFormula;
    }

    public static boolean isEquationMode() {
        return equationMode;
    }

    public static void resetCache() {
        parsedFormula = null;
        lastFormula = null;
        equationMode = false;
    }
}