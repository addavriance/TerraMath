package me.adda.terramath.api;

import me.adda.terramath.math.ParsedFormula;

public class FormulaCacheHolder {
    private static ParsedFormula parsedFormula = null;
    private static String lastFormula = null;

    public static ParsedFormula getParsedFormula() {
        String currentFormula = TerraFormulaManager.getInstance().getFormula();
        if ((parsedFormula == null || !currentFormula.equals(lastFormula)) && !currentFormula.trim().isEmpty()) {
            parsedFormula = ParsedFormula.parse(currentFormula);
            lastFormula = currentFormula;
        }
        return parsedFormula;
    }

    public static void resetCache() {
        parsedFormula = null;
        lastFormula = null;
    }
}