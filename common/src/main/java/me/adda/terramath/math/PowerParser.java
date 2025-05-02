package me.adda.terramath.math;

public class PowerParser {

    private String expression;

    public PowerParser(String expression) {
        this.expression = expression;
    }

    public String parse() {
        int i = 0;

        while (i < expression.length()) {
            if (expression.charAt(i) == '^') {
                int operandEnd = i;
                int operandStart = i - 1;

                while (operandStart >= 0 && Character.isWhitespace(expression.charAt(operandStart))) {
                    operandStart--;
                }

                if (operandStart < 0) {
                    i++;
                    continue;
                }

                if (Character.isLetterOrDigit(expression.charAt(operandStart)) || expression.charAt(operandStart) == ')') {
                    String operand1;

                    if (expression.charAt(operandStart) == ')') {
                        int bracketCount = 1;
                        int j = operandStart - 1;

                        while (j >= 0 && bracketCount > 0) {
                            if (expression.charAt(j) == '(') {
                                bracketCount--;
                            } else if (expression.charAt(j) == ')') {
                                bracketCount++;
                            }
                            j--;
                        }

                        if (bracketCount == 0) {
                            int openBracketPos = j + 1;

                            if (ParserUtils.isFunction(expression, openBracketPos)) {
                                int funcStart = ParserUtils.findFunctionStart(expression, openBracketPos);
                                operand1 = expression.substring(funcStart, operandEnd);
                            } else {
                                operand1 = expression.substring(openBracketPos, operandEnd);
                            }
                        } else {
                            i++;
                            continue;
                        }
                    } else {
                        int j = operandStart;
                        while (j >= 0 && (Character.isLetterOrDigit(expression.charAt(j)) || expression.charAt(j) == '.')) {
                            j--;
                        }
                        operand1 = expression.substring(j + 1, operandEnd);
                    }

                    // Find the second operand
                    int operand2Start = i + 1;
                    int operand2End = operand2Start;

                    while (operand2End < expression.length() && (Character.isLetterOrDigit(expression.charAt(operand2End)) || expression.charAt(operand2End) == '.' || expression.charAt(operand2End) == '(')) {
                        if (expression.charAt(operand2End) == '(') {
                            int bracketCount = 1;
                            int k = operand2End + 1;

                            while (k < expression.length() && bracketCount > 0) {
                                if (expression.charAt(k) == '(') {
                                    bracketCount++;
                                } else if (expression.charAt(k) == ')') {
                                    bracketCount--;
                                }
                                k++;
                            }

                            if (bracketCount == 0) {
                                operand2End = k;
                            }
                        } else {
                            operand2End++;
                        }
                    }

                    String operand2 = expression.substring(operand2Start, operand2End);

                    expression = expression.replace(operand1 + "^" + operand2, "Math.pow(" + operand1 + "," + operand2 + ")");
                } else {
                    i++;
                    continue;
                }
            }

            i++;
        }

        return expression;
    }
}
