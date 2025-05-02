package me.adda.terramath.api;

public class TerrainFormulaManager {
    private static final TerrainFormulaManager INSTANCE = new TerrainFormulaManager();
    private String currentFormula = "";

    private TerrainFormulaManager() {}

    public static TerrainFormulaManager getInstance() {
        return INSTANCE;
    }

    public String getFormula() {
        return currentFormula;
    }

    public void setFormula(String formula) {
        this.currentFormula = formula;
    }
}