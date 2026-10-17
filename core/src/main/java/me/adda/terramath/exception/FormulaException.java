package me.adda.terramath.exception;

public class FormulaException extends IllegalArgumentException {
    private final Object[] args;

    public FormulaException(String message) {
        super(message);
        this.args = new Object[0];
    }

    public FormulaException(String message, Object... args) {
        super(message);
        this.args = args;
    }

    public Object[] getArgs() {
        return args;
    }
}