package me.adda.terramath.math;

import me.adda.terramath.exception.FormulaException;

import java.util.ArrayList;
import java.util.List;

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

    public double evaluate(double x, double y, double z) {
        return root.evaluate(new Variables(x, y, z));
    }

    public String getOriginalExpression() {
        return originalExpression;
    }

    private static class Variables {
        final double x, y, z;

        Variables(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private interface ExpressionNode {
        double evaluate(Variables vars);
    }

    private static class NumberNode implements ExpressionNode {
        private final double value;
        NumberNode(double value) { this.value = value; }
        @Override
        public double evaluate(Variables vars) { return value; }
    }

    private static class VariableNode implements ExpressionNode {
        private final char variable;

        VariableNode(char variable) {
            this.variable = variable;
        }

        @Override
        public double evaluate(Variables vars) {
            return switch (variable) {
                case 'x' -> vars.x;
                case 'y' -> vars.y;
                case 'z' -> vars.z;
                default -> throw new IllegalStateException("Unknown variable: " + variable);
            };
        }
    }

    private static class BinaryOperationNode implements ExpressionNode {
        private final char operator;
        private final ExpressionNode left, right;

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
                case '^' -> {
                    if (Double.isNaN(leftValue) || Double.isNaN(rightValue)) {
                        throw new FormulaException(FormulaParser.ERROR_INVALID_POWER);
                    }
                    yield Math.pow(leftValue, rightValue);
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
            List<Double> args = arguments.stream()
                    .map(arg -> arg.evaluate(vars))
                    .toList();

            return switch (name) {
                case "sin" -> Math.sin(args.get(0));
                case "cos" -> Math.cos(args.get(0));
                case "tan" -> Math.tan(args.get(0));
                case "asin" -> Math.asin(args.get(0));
                case "acos" -> Math.acos(args.get(0));
                case "atan" -> Math.atan(args.get(0));

                case "sinh" -> Math.sinh(args.get(0));
                case "cosh" -> Math.cosh(args.get(0));
                case "tanh" -> Math.tanh(args.get(0));

                case "sqrt" -> Math.sqrt(args.get(0));
                case "cbrt" -> Math.cbrt(args.get(0));
                case "pow" -> Math.pow(args.get(0), args.get(1));

                case "ln" -> Math.log(args.get(0));
                case "lg" -> Math.log10(args.get(0));

                case "abs" -> Math.abs(args.get(0));
                case "exp" -> Math.exp(args.get(0));
                case "floor" -> Math.floor(args.get(0));
                case "ceil" -> Math.ceil(args.get(0));
                case "round" -> Math.round(args.get(0));
                case "sign" -> Math.signum(args.get(0));

                case "gamma" -> MathExtensions.gamma(args.get(0));
                case "erf" -> MathExtensions.erf(args.get(0));
                case "beta" -> MathExtensions.beta(args.get(0), args.get(1));

                case "mod" -> args.get(0) % args.get(1);
                case "max" -> Math.max(args.get(0), args.get(1));
                case "min" -> Math.min(args.get(0), args.get(1));
                case "sigmoid" -> 1.0 / (1.0 + Math.exp(-args.get(0)));
                case "clamp" -> Math.min(Math.max(args.get(0), args.get(1)), args.get(2));

                case "noise" -> MathExtensions.noise(args.get(0), args.get(1), args.get(2));

                default -> throw new FormulaException(FormulaParser.ERROR_UNKNOWN_FUNCTION, name);
            };
        }
    }

    private record UnaryMinusNode(ExpressionNode operand) implements ExpressionNode {

        @Override
        public double evaluate(Variables vars) {
            return -operand.evaluate(vars);
        }
    }

    private static class FactorialNode implements ExpressionNode {
        private final ExpressionNode operand;

        FactorialNode(ExpressionNode operand) {
            this.operand = operand;
        }

        @Override
        public double evaluate(Variables vars) {
            double value = operand.evaluate(vars);

            if (value < 0) {
                return Double.NaN;
            }

            if (value == Math.floor(value) && value <= 170) {
                double result = 1;
                for (int i = 2; i <= value; i++) {
                    result *= i;
                }
                return result;
            }

            // Γ(n+1) = n!
            return MathExtensions.gamma(value + 1);
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
            return parseBinaryOperation(0);
        }

        private ExpressionNode parseBinaryOperation(int minPrecedence) {
            ExpressionNode left = parsePrimary();

            while (position < length) {
                char op = peek();
                Integer precedence = FormulaParser.OPERATOR_PRECEDENCE.get(op);

                if (precedence == null || precedence < minPrecedence) {
                    break;
                }

                consume();
                ExpressionNode right = parseBinaryOperation(precedence + 1);
                left = new BinaryOperationNode(op, left, right);
            }

            return left;
        }

        private ExpressionNode parsePrimary() {
            char ch = peek();

            if (ch == '(') {
                consume();
                ExpressionNode node = parseExpression();
                expect(')', FormulaParser.ERROR_MISSING_CLOSING);

                if (position < length && peek() == '!') {
                    consume();
                    return new FactorialNode(node);
                }

                return node;
            }

            if (ch == '-') {
                consume();
                return new UnaryMinusNode(parsePrimary());
            }

            if (ch == '+') {
                consume();
                return parsePrimary();
            }

            if (ch == 'x' || ch == 'y' || ch == 'z') {
                consume();
                ExpressionNode node = new VariableNode(ch);

                if (position < length && peek() == '!') {
                    consume();
                    return new FactorialNode(node);
                }

                return node;
            }

            if (Character.isLetter(ch)) {
                return parseFunction();
            }

            ExpressionNode node = parseNumber();

            if (position < length && peek() == '!') {
                consume();
                return new FactorialNode(node);
            }

            return node;
        }


        private ExpressionNode parseFunction() {
            String name = parseFunctionName();
            if (!FormulaParser.FUNCTIONS.contains(name)) {
                throw new FormulaException(FormulaParser.ERROR_UNKNOWN_FUNCTION, name);
            }

            expect('(', FormulaParser.ERROR_MISSING_OPENING);
            List<ExpressionNode> args = parseArguments();
            expect(')', FormulaParser.ERROR_MISSING_CLOSING);

            validateFunctionArguments(name, args);
            return new FunctionNode(name, args);
        }

        private String parseFunctionName() {
            StringBuilder name = new StringBuilder();
            while (position < length && Character.isLetter(peek())) {
                name.append(consume());
            }
            return name.toString().toLowerCase();
        }

        private List<ExpressionNode> parseArguments() {
            List<ExpressionNode> args = new ArrayList<>();
            while (true) {
                args.add(parseExpression());
                if (peek() == ')') break;
                expect(',', FormulaParser.ERROR_ARGUMENT_SEPARATOR);
            }
            return args;
        }

        private void validateFunctionArguments(String name, List<ExpressionNode> args) {
            int expectedArgs = switch (name) {
                case "pow", "mod", "max", "min", "beta" -> 2;
                case "clamp", "noise" -> 3;
                default -> 1;
            };

            if (args.size() != expectedArgs) {
                switch (expectedArgs) {
                    case 3:
                        throw new FormulaException(FormulaParser.ERROR_THREE_ARGUMENTS, name);
                    case 2:
                        throw new FormulaException(FormulaParser.ERROR_TWO_ARGUMENTS, name);
                    case 1:
                        throw new FormulaException(FormulaParser.ERROR_ARGUMENTS, name);
                }
            }
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

        private void expect(char expected, String errorKey) {
            if (peek() != expected) {
                throw new FormulaException(errorKey);
            }
            consume();
        }
    }
}