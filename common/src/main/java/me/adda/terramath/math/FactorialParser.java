package me.adda.terramath.math;

import me.adda.terramath.math.ParserUtils;

public class FactorialParser {

    private String expression;

    public FactorialParser(String expression) {
        this.expression = expression;
    }

    public String parse() {
        int i = 0;

        while (i < expression.length()) {
            if (expression.charAt(i) == '!') {
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
                    String operand;

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
                                operand = expression.substring(funcStart, operandEnd);
                            } else {
                                operand = expression.substring(openBracketPos, operandEnd);
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
                        operand = expression.substring(j + 1, operandEnd);
                    }

                    expression = expression.replace(operand + "!", "MathExtensions.gamma(" + operand + "+1)");
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
