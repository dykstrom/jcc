/*
 * Copyright (C) 2019 Johan Dykstrom
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.dykstrom.jcc.common.optimization;

import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.InvalidException;
import se.dykstrom.jcc.common.types.I64;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * The default expression optimizer that performs AST optimizations applicable for all programming languages.
 *
 * @author Johan Dykstrom
 */
public class DefaultAstExpressionOptimizer implements AstExpressionOptimizer {

    private final TypeManager typeManager;

    public DefaultAstExpressionOptimizer(TypeManager typeManager) {
        this.typeManager = typeManager;
    }

    @Override
    public Expression expression(Expression expression) {
        if (expression instanceof BinaryExpression) {
            BinaryExpression binaryExpression = (BinaryExpression) expression;
            int line = binaryExpression.line();
            int column = binaryExpression.column();
            Expression left = expression(binaryExpression.getLeft());
            Expression right = expression(binaryExpression.getRight());

            if (expression instanceof AddExpression) {
                return addExpression(line, column, left, right);
            } else if (expression instanceof SubExpression) {
                return subExpression(line, column, left, right);
            } else if (expression instanceof MulExpression) {
                return mulExpression(line, column, left, right);
            } else if (expression instanceof DivExpression) {
                return divExpression(line, column, left, right);
            } else if (expression instanceof IDivExpression) {
                return idivExpression(line, column, left, right);
            }

            return binaryExpression.withLeft(left).withRight(right);
        } else if (expression instanceof FunctionCallExpression) {
            FunctionCallExpression functionCall = (FunctionCallExpression) expression;
            List<Expression> args = functionCall.getArgs().stream().map(this::expression).collect(toList());
            return functionCall.withArgs(args);
        }
        return expression;
    }

    /**
     * Simplifies an add expression to a literal expression if possible.
     */
    private Expression addExpression(int line, int column, Expression left, Expression right) {
        if (isZero(left)) {
            return right;
        } else if (isZero(right)) {
            return left;
        } else if (isIntegerLiteral(left) && isIntegerLiteral(right)) {
            return new IntegerLiteral(line, column, asLong(left) + asLong(right));
        } else if (isNumericLiteral(left) && isNumericLiteral(right)) {
            return new FloatLiteral(line, column, asDouble(left) + asDouble(right));
        } else if (isStringLiteral(left) && isStringLiteral(right)) {
            return new StringLiteral(line, column, asString(left) + asString(right));
        }
        return new AddExpression(line, column, left, right);
    }

    /**
     * Simplifies a sub expression to a literal expression if possible.
     */
    private Expression subExpression(int line, int column, Expression left, Expression right) {
        if (isZero(left) && isIntegerLiteral(right)) {
            return new IntegerLiteral(right.line(), right.column(), -asLong(right));
        } else if (isZero(left) && isNumericLiteral(right)) {
            return new FloatLiteral(right.line(), right.column(), -asDouble(right));
        } else if (isZero(right)) {
            return left;
        } else if (isIntegerLiteral(left) && isIntegerLiteral(right)) {
            return new IntegerLiteral(line, column, asLong(left) - asLong(right));
        } else if (isNumericLiteral(left) && isNumericLiteral(right)) {
            return new FloatLiteral(line, column, asDouble(left) - asDouble(right));
        }
        return new SubExpression(line, column, left, right);
    }

    /**
     * Simplifies a mul expression to a literal expression if possible. Otherwise, tries to replace
     * the multiplication with a shift expression if that is possible.
     */
    private Expression mulExpression(int line, int column, Expression left, Expression right) {
        if (isZero(left) && hasNoFunctionCall(right)) {
            return new IntegerLiteral(line, column, 0);
        } else if (isZero(right) && hasNoFunctionCall(left)) {
            return new IntegerLiteral(line, column, 0);
        } else if (isOne(left)) {
            return right;
        } else if (isOne(right)) {
            return left;
        } else if (isIntegerLiteral(left) && isIntegerLiteral(right)) {
            return new IntegerLiteral(line, column, asLong(left) * asLong(right));
        } else if (isNumericLiteral(left) && isNumericLiteral(right)) {
            return new FloatLiteral(line, column, asDouble(left) * asDouble(right));
        } else if (isPowerOfTwo(left) && isIntegerType(right)) {
            return new ShiftLeftExpression(line, column, right, new IntegerLiteral(line, column, asShift(left)));
        } else if (isIntegerType(left) && isPowerOfTwo(right)) {
            return new ShiftLeftExpression(line, column, left, new IntegerLiteral(line, column, asShift(right)));
        }
        return new MulExpression(line, column, left, right);
    }

    /**
     * Simplifies a div expression to a literal expression if possible.
     */
    private Expression divExpression(int line, int column, Expression left, Expression right) {
        if (isZero(left) && hasNoFunctionCall(right)) {
            return new FloatLiteral(line, column, 0.0);
        } else if (isZero(right)) {
            throw new InvalidException("division by zero: " + right, right.toString());
        } else if (isOne(right)) {
            return left;
        } else if (isNumericLiteral(left) && isNumericLiteral(right)) {
            return new FloatLiteral(line, column, asDouble(left) / asDouble(right));
        }
        return new DivExpression(line, column, left, right);
    }

    /**
     * Simplifies an idiv (integer division) expression to a literal expression if possible.
     */
    private Expression idivExpression(int line, int column, Expression left, Expression right) {
        if (isZero(left) && hasNoFunctionCall(right)) {
            return new IntegerLiteral(line, column, 0);
        } else if (isZero(right)) {
            throw new InvalidException("division by zero: " + right, right.toString());
        } else if (isOne(right)) {
            return left;
        } else if (isIntegerLiteral(left) && isIntegerLiteral(right)) {
            return new IntegerLiteral(line, column, asLong(left) / asLong(right));
        }
        return new IDivExpression(line, column, left, right);
    }

    /**
     * Returns {@code true} if the given expression does not contain any function calls.
     */
    private boolean hasNoFunctionCall(Expression expression) {
        if (expression instanceof FunctionCallExpression) {
            return false;
        }
        if (expression instanceof UnaryExpression) {
            return hasNoFunctionCall(((UnaryExpression) expression).getExpression());
        }
        if (expression instanceof BinaryExpression) {
            return hasNoFunctionCall(((BinaryExpression) expression).getLeft()) && hasNoFunctionCall(((BinaryExpression) expression).getRight());
        }
        return true;
    }

    /**
     * Returns the given expression, which has to be a literal expression, as a long.
     */
    private static long asLong(Expression expression) {
        return Long.parseLong(((LiteralExpression) expression).getValue());
    }

    /**
     * Returns the given expression, which has to be a literal expression, as a double.
     */
    private static double asDouble(Expression expression) {
        return Double.parseDouble(((LiteralExpression) expression).getValue());
    }

    /**
     * Returns the given expression, which has to be a literal expression, as a string.
     */
    private static String asString(Expression expression) {
        return ((StringLiteral) expression).getValue();
    }

    /**
     * Returns the given expression, which has to be an integer literal that is a power of two,
     * as the number of shifts needed to satisfy the relation:
     *
     * x * (literal value) == x << (number of shifts)
     */
    private int asShift(Expression expression) {
        if (!isPowerOfTwo(expression)) {
            throw new IllegalArgumentException("expression is not a power of two: " + expression);
        }
        long value = ((IntegerLiteral) expression).asLong();
        int shift = 0;
        while ((1L << shift) != value) {
            shift++;
        }
        return shift;
    }

    /**
     * Returns {@code true} if the given expression is an integer literal with a value that is a power of two.
     */
    private static boolean isPowerOfTwo(Expression expression) {
        if (expression instanceof IntegerLiteral) {
            long value = ((IntegerLiteral) expression).asLong();
            return (value != 0) && ((value & (value - 1)) == 0);
        }
        return false;
    }

    /**
     * Returns {@code true} if the given expression is a literal expression that evaluates to 0.
     */
    private static boolean isZero(Expression expression) {
        if (expression instanceof IntegerLiteral) {
            return ((IntegerLiteral) expression).asLong() == 0;
        } else if (expression instanceof FloatLiteral) {
            return Double.compare(((FloatLiteral) expression).asDouble(), 0.0) == 0;
        }
        return false;
    }

    /**
     * Returns {@code true} if the given expression is a literal expression that evaluates to 1.
     */
    private static boolean isOne(Expression expression) {
        if (expression instanceof IntegerLiteral) {
            return ((IntegerLiteral) expression).asLong() == 1;
        } else if (expression instanceof FloatLiteral) {
            return Double.compare(((FloatLiteral) expression).asDouble(), 1.0) == 0;
        }
        return false;
    }

    /**
     * Returns {@code true} if the type of the given expression is an integer type.
     */
    private boolean isIntegerType(Expression expression) {
        return typeManager.getType(expression) instanceof I64;
    }

    private static boolean isIntegerLiteral(Expression expression) {
        return expression instanceof IntegerLiteral;
    }

    private static boolean isNumericLiteral(Expression expression) {
        return expression instanceof IntegerLiteral || expression instanceof FloatLiteral;
    }

    private static boolean isStringLiteral(Expression expression) {
        return expression instanceof StringLiteral;
    }
}
