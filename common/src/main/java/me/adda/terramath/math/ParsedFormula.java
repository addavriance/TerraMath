package me.adda.terramath.math;

import me.adda.terramath.exception.FormulaException;

import java.util.*;

public class ParsedFormula {
    private final String originalExpression;
    private final ExpressionNode root;

    private ParsedFormula(String expression, ExpressionNode root) {
        this.originalExpression = expression;
        this.root = root;
    }

    public static ParsedFormula parse(String expression) {
        ExpressionParser parser = new ExpressionParser(expression);
        return new ParsedFormula(expression, parser.parse());
    }

    public double evaluate(double x, double z) {
        return root.evaluate(new Variables(x, z));
    }

    public String getOriginalExpression() {
        return originalExpression;
    }

    private static class Variables {
        final double x;
        final double z;

        Variables(double x, double z) {
            this.x = x;
            this.z = z;
        }
    }

    private interface ExpressionNode {
        double evaluate(Variables vars);
    }

    private static class NumberNode implements ExpressionNode {
        private final double value;

        NumberNode(double value) {
            this.value = value;
        }

        @Override
        public double evaluate(Variables vars) {
            return value;
        }
    }

    private static class VariableNode implements ExpressionNode {
        private final boolean isX;

        VariableNode(boolean isX) {
            this.isX = isX;
        }

        @Override
        public double evaluate(Variables vars) {
            return isX ? vars.x : vars.z;
        }
    }

    private static class BinaryOperationNode implements ExpressionNode {
        private final char operator;
        private final ExpressionNode left;
        private final ExpressionNode right;

        BinaryOperationNode(char operator, ExpressionNode left, ExpressionNode right) {
            this.operator = operator;
            this.left = left;
            this.right = right;
        }

        @Override
        public double evaluate(Variables vars) {
            double leftValue = left.evaluate(vars);
            double rightValue = right.evaluate(vars);

            return switch (operator) {
                case '+' -> leftValue + rightValue;
                case '-' -> leftValue - rightValue;
                case '*' -> leftValue * rightValue;
                case '/' -> {
                    if (Math.abs(rightValue) < 1e-10) {
                        throw new FormulaException(FormulaParser.ERROR_DIVISION_ZERO);
                    }
                    yield leftValue / rightValue;
                }
                default -> throw new IllegalStateException("Unknown operator: " + operator);
            };
        }
    }

    private static class FunctionNode implements ExpressionNode {
        private final String name;
        private final List<ExpressionNode> arguments;

        FunctionNode(String name, List<ExpressionNode> arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        @Override
        public double evaluate(Variables vars) {
            List<Double> evaluatedArgs = arguments.stream()
                    .map(arg -> arg.evaluate(vars))
                    .toList();

            return switch (name) {
                case "sin" -> Math.sin(evaluatedArgs.get(0));
                case "cos" -> Math.cos(evaluatedArgs.get(0));
                case "tan" -> Math.tan(evaluatedArgs.get(0));
                case "sqrt" -> Math.sqrt(evaluatedArgs.get(0));
                case "abs" -> Math.abs(evaluatedArgs.get(0));
                case "pow" -> Math.pow(evaluatedArgs.get(0), evaluatedArgs.get(1));
                default -> throw new FormulaException(FormulaParser.ERROR_UNKNOWN_FUNCTION, name);
            };
        }
    }

    private static class UnaryMinusNode implements ExpressionNode {
        private final ExpressionNode operand;

        UnaryMinusNode(ExpressionNode operand) {
            this.operand = operand;
        }

        @Override
        public double evaluate(Variables vars) {
            return -operand.evaluate(vars);
        }
    }

    private static class ExpressionParser {
        private final String expression;
        private int position;
        private final int length;

        public ExpressionParser(String expression) {
            this.expression = expression.replaceAll("\\s+", "");
            this.length = this.expression.length();
            this.position = 0;
        }

        public ExpressionNode parse() {
            ExpressionNode result = parseExpression();
            if (position < length) {
                throw new FormulaException(FormulaParser.ERROR_UNEXPECTED_CHAR,
                        expression.charAt(position), position);
            }
            return result;
        }

        private ExpressionNode parseExpression() {
            ExpressionNode left = parseTerm();

            while (position < length) {
                char op = peek();
                if (op != '+' && op != '-') {
                    break;
                }

                consume();
                ExpressionNode right = parseTerm();
                left = new BinaryOperationNode(op, left, right);
            }

            return left;
        }

        private ExpressionNode parseTerm() {
            ExpressionNode left = parseFactor();

            while (position < length) {
                char op = peek();
                if (op != '*' && op != '/') {
                    break;
                }

                consume();
                ExpressionNode right = parseFactor();
                left = new BinaryOperationNode(op, left, right);
            }

            return left;
        }

        private ExpressionNode parseFactor() {
            char ch = peek();

            if (ch == '(') {
                consume();
                ExpressionNode node = parseExpression();
                if (position >= length || peek() != ')') {
                    throw new FormulaException(FormulaParser.ERROR_MISSING_CLOSING);
                }
                consume();
                return node;
            }

            if (ch == '-') {
                consume();
                return new UnaryMinusNode(parseFactor());
            }

            if (ch == '+') {
                consume();
                return parseFactor();
            }

            if (ch == 'x') {
                consume();
                return new VariableNode(true);
            }

            if (ch == 'z') {
                consume();
                return new VariableNode(false);
            }

            StringBuilder funcName = new StringBuilder();
            while (position < length && Character.isLetter(peek())) {
                funcName.append(consume());
            }

            if (!funcName.isEmpty()) {
                String name = funcName.toString().toLowerCase();
                if (!FormulaParser.FUNCTIONS.contains(name)) {
                    throw new FormulaException(FormulaParser.ERROR_UNKNOWN_FUNCTION, name);
                }

                if (peek() != '(') {
                    throw new FormulaException(FormulaParser.ERROR_MISSING_OPENING, name);
                }
                consume();

                List<ExpressionNode> args = new ArrayList<>();
                while (true) {
                    args.add(parseExpression());
                    if (peek() == ')') {
                        break;
                    }
                    if (peek() != ',') {
                        throw new FormulaException(FormulaParser.ERROR_ARGUMENT_SEPARATOR);
                    }
                    consume();
                }
                consume();

                if (name.equals("pow") && args.size() != 2) {
                    throw new FormulaException(FormulaParser.ERROR_POW_ARGUMENTS);
                } else if (!name.equals("pow") && args.size() != 1) {
                    throw new FormulaException(FormulaParser.ERROR_ARGUMENTS, name);
                }

                return new FunctionNode(name, args);
            }

            return parseNumber();
        }

        private ExpressionNode parseNumber() {
            StringBuilder sb = new StringBuilder();
            while (position < length && (Character.isDigit(peek()) || peek() == '.')) {
                sb.append(consume());
            }

            if (sb.isEmpty()) {
                throw new FormulaException(FormulaParser.ERROR_EXPECTED_NUMBER, position);
            }

            try {
                return new NumberNode(Double.parseDouble(sb.toString()));
            } catch (NumberFormatException e) {
                throw new FormulaException(FormulaParser.ERROR_INVALID_NUMBER, sb.toString());
            }
        }

        private char peek() {
            return position < length ? expression.charAt(position) : '\0';
        }

        private char consume() {
            return expression.charAt(position++);
        }
    }
}