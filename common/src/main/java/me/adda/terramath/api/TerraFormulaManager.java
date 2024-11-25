package me.adda.terramath.api;

public class TerraFormulaManager {
    private static final TerraFormulaManager INSTANCE = new TerraFormulaManager();
    private String currentFormula = "";

    private TerraFormulaManager() {}

    public static TerraFormulaManager getInstance() {
        return INSTANCE;
    }

    public String getFormula() {
        return currentFormula;
    }

    public void setFormula(String formula) {
        this.currentFormula = formula;
    }
}