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
import se.dykstrom.jcc.common.error.InvalidValueException;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;

import java.util.List;
import java.util.function.BiPredicate;

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
    public Expression expression(final Expression expression, final SymbolTable symbols) {
        if (expression instanceof UnaryExpression unaryExpression) {
            return unaryExpression(symbols, unaryExpression);
        } else if (expression instanceof BinaryExpression binaryExpression) {
            return binaryExpression(symbols, binaryExpression);
        } else if (expression instanceof FunctionCallExpression functionCall) {
            List<Expression> args = functionCall.getArgs().stream().map(expr -> expression(expr, symbols)).toList();
            return functionCall.withArgs(args);
        } else if (expression instanceof IdentifierDerefExpression ide &&
                symbols.contains(ide.getIdentifier().name()) &&
                symbols.isConstant(ide.getIdentifier().name())) {
            final var line = ide.line();
            final var column = ide.column();
            final var type = ide.getIdentifier().type();
            final var value = symbols.getValue(ide.getIdentifier().name());

            if (type instanceof I64) {
                return new IntegerLiteral(line, column, (String) value);
            } else if (type instanceof F64) {
                return new FloatLiteral(line, column, (String) value);
            } else if (type instanceof Str) {
                return new StringLiteral(line, column, (String) value);
            }
        }
        return expression;
    }

    protected Expression unaryExpression(final SymbolTable symbols, final UnaryExpression unaryExpression) {
        final var line = unaryExpression.line();
        final var column = unaryExpression.column();
        final var expr = expression(unaryExpression.getExpression(), symbols);

        if (unaryExpression instanceof NegateExpression) {
            return negateExpression(line, column, expr);
        } else if (unaryExpression instanceof NotExpression) {
            return notExpression(line, column, expr);
        }

        return unaryExpression.withExpression(expr);
    }

    /**
     * Simplifies the given negate expression to a literal expression if possible.
     */
    private Expression negateExpression(final int line, final int column, final Expression expr) {
        if (isIntegerLiteral(expr)) {
            return new IntegerLiteral(line, column, -asLong(expr));
        } else if (isNumericLiteral(expr)) {
            return new FloatLiteral(line, column, -asDouble(expr));
        }
        return new NegateExpression(line, column, expr);
    }

    /**
     * Simplifies a NOT expression to a literal expression if possible.
     */
    private Expression notExpression(final int line, final int column, final Expression expr) {
        if (isIntegerLiteral(expr)) {
            return new IntegerLiteral(line, column, ~asLong(expr));
        }
        return new NotExpression(line, column, expr);
    }

    private Expression binaryExpression(final SymbolTable symbols, final BinaryExpression binaryExpression) {
        int line = binaryExpression.line();
        int column = binaryExpression.column();
        Expression left = expression(binaryExpression.getLeft(), symbols);
        Expression right = expression(binaryExpression.getRight(), symbols);

        if (binaryExpression instanceof AddExpression) {
            return addExpression(line, column, left, right);
        } else if (binaryExpression instanceof SubExpression) {
            return subExpression(line, column, left, right);
        } else if (binaryExpression instanceof MulExpression) {
            return mulExpression(line, column, left, right);
        } else if (binaryExpression instanceof DivExpression) {
            return divExpression(line, column, left, right);
        } else if (binaryExpression instanceof IDivExpression) {
            return idivExpression(line, column, left, right);
        } else if (binaryExpression instanceof ModExpression) {
            return modExpression(line, column, left, right);
        } else if (binaryExpression instanceof AndExpression) {
            return andExpression(line, column, left, right);
        } else if (binaryExpression instanceof OrExpression) {
            return orExpression(line, column, left, right);
        } else if (binaryExpression instanceof XorExpression) {
            return xorExpression(line, column, left, right);
        } else if (binaryExpression instanceof EqualExpression) {
            return eqExpression(line, column, left, right);
        } else if (binaryExpression instanceof NotEqualExpression) {
            return neExpression(line, column, left, right);
        } else if (binaryExpression instanceof LessExpression) {
            return ltExpression(line, column, left, right);
        } else if (binaryExpression instanceof LessOrEqualExpression) {
            return leExpression(line, column, left, right);
        } else if (binaryExpression instanceof GreaterExpression) {
            return gtExpression(line, column, left, right);
        } else if (binaryExpression instanceof GreaterOrEqualExpression) {
            return geExpression(line, column, left, right);
        }

        return binaryExpression.withLeft(left).withRight(right);
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
            throw new InvalidValueException("division by zero: " + right, right.toString());
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
            throw new InvalidValueException("division by zero: " + right, right.toString());
        } else if (isOne(right)) {
            return left;
        } else if (isIntegerLiteral(left) && isIntegerLiteral(right)) {
            return new IntegerLiteral(line, column, asLong(left) / asLong(right));
        }
        return new IDivExpression(line, column, left, right);
    }

    /**
     * Simplifies a MOD expression to a literal expression if possible.
     */
    private Expression modExpression(int line, int column, Expression left, Expression right) {
        if (isZero(left) && hasNoFunctionCall(right)) {
            return new IntegerLiteral(line, column, 0);
        } else if (isZero(right)) {
            throw new InvalidValueException("division by zero: " + right, right.toString());
        } else if (isOne(right)) {
            return new IntegerLiteral(line, column, 0);
        } else if (isIntegerLiteral(left) && isIntegerLiteral(right)) {
            return new IntegerLiteral(line, column, asLong(left) % asLong(right));
        }
        return new ModExpression(line, column, left, right);
    }

    /**
     * Simplifies an AND expression to a literal expression if possible.
     */
    private Expression andExpression(int line, int column, Expression left, Expression right) {
        if (isZero(left) && hasNoFunctionCall(right)) {
            return new IntegerLiteral(line, column, 0);
        } else if (isZero(right) && hasNoFunctionCall(left)) {
            return new IntegerLiteral(line, column, 0);
        } else if (isIntegerLiteral(left) && isIntegerLiteral(right)) {
            return new IntegerLiteral(line, column, asLong(left) & asLong(right));
        }
        return new AndExpression(line, column, left, right);
    }

    /**
     * Simplifies an OR expression to a literal expression if possible.
     */
    private Expression orExpression(int line, int column, Expression left, Expression right) {
        if (isZero(left)) {
            return right;
        } else if (isZero(right)) {
            return left;
        } else if (isIntegerLiteral(left) && isIntegerLiteral(right)) {
            return new IntegerLiteral(line, column, asLong(left) | asLong(right));
        }
        return new OrExpression(line, column, left, right);
    }

    /**
     * Simplifies an XOR expression to a literal expression if possible.
     */
    private Expression xorExpression(int line, int column, Expression left, Expression right) {
        if (isZero(left)) {
            return right;
        } else if (isZero(right)) {
            return left;
        } else if (isIntegerLiteral(left) && isIntegerLiteral(right)) {
            return new IntegerLiteral(line, column, asLong(left) ^ asLong(right));
        }
        return new XorExpression(line, column, left, right);
    }

    /**
     * Simplifies an EQ expression to a literal expression if possible.
     */
    private Expression eqExpression(int line, int column, Expression left, Expression right) {
        final var result = compare(left, right, (Long a, Long b) -> a.longValue() == b.longValue());
        if (result != null) {
            return new IntegerLiteral(line, column, result);
        } else {
            return new EqualExpression(line, column, left, right);
        }
    }

    /**
     * Simplifies an NE expression to a literal expression if possible.
     */
    private Expression neExpression(int line, int column, Expression left, Expression right) {
        final var result = compare(left, right, (Long a, Long b) -> a.longValue() != b.longValue());
        if (result != null) {
            return new IntegerLiteral(line, column, result);
        } else {
            return new NotEqualExpression(line, column, left, right);
        }
    }

    /**
     * Simplifies an LT expression to a literal expression if possible.
     */
    private Expression ltExpression(int line, int column, Expression left, Expression right) {
        final var result = compare(left, right, (Long a, Long b) -> a < b);
        if (result != null) {
            return new IntegerLiteral(line, column, result);
        } else {
            return new LessExpression(line, column, left, right);
        }
    }

    /**
     * Simplifies an LE expression to a literal expression if possible.
     */
    private Expression leExpression(int line, int column, Expression left, Expression right) {
        final var result = compare(left, right, (Long a, Long b) -> a <= b);
        if (result != null) {
            return new IntegerLiteral(line, column, result);
        } else {
            return new LessOrEqualExpression(line, column, left, right);
        }
    }

    /**
     * Simplifies a GT expression to a literal expression if possible.
     */
    private Expression gtExpression(int line, int column, Expression left, Expression right) {
        final var result = compare(left, right, (Long a, Long b) -> a > b);
        if (result != null) {
            return new IntegerLiteral(line, column, result);
        } else {
            return new GreaterExpression(line, column, left, right);
        }
    }

    /**
     * Simplifies a GE expression to a literal expression if possible.
     */
    private Expression geExpression(int line, int column, Expression left, Expression right) {
        final var result = compare(left, right, (Long a, Long b) -> a >= b);
        if (result != null) {
            return new IntegerLiteral(line, column, result);
        } else {
            return new GreaterOrEqualExpression(line, column, left, right);
        }
    }

    private Long compare(final Expression left, final Expression right, final BiPredicate<Long, Long> predicate) {
        final boolean result;
        if (isIntegerLiteral(left) && isIntegerLiteral(right)) {
            result = predicate.test(asLong(left), asLong(right));
        } else if (isIntegerLiteral(left) && isFloatLiteral(right)) {
            result = predicate.test((long) Double.compare(asLong(left), asDouble(right)), 0L);
        } else if (isFloatLiteral(left) && isIntegerLiteral(right)) {
            result = predicate.test((long) Double.compare(asDouble(left), asLong(right)), 0L);
        } else if (isFloatLiteral(left) && isFloatLiteral(right)) {
            result = predicate.test((long) Double.compare(asDouble(left), asDouble(right)), 0L);
        } else if (isStringLiteral(left) && isStringLiteral(right)) {
            result = predicate.test((long) asString(left).compareTo(asString(right)), 0L);
        } else {
            return null;
        }
        return result ? -1L : 0L;
    }

    /**
     * Returns {@code true} if the given expression does not contain any function calls.
     */
    private boolean hasNoFunctionCall(Expression expression) {
        if (expression instanceof FunctionCallExpression) {
            return false;
        }
        if (expression instanceof UnaryExpression unaryExpression) {
            return hasNoFunctionCall(unaryExpression.getExpression());
        }
        if (expression instanceof BinaryExpression binaryExpression) {
            return hasNoFunctionCall(binaryExpression.getLeft()) && hasNoFunctionCall(binaryExpression.getRight());
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
     * <p>
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
        if (expression instanceof IntegerLiteral integerLiteral) {
            long value = integerLiteral.asLong();
            return (value != 0) && ((value & (value - 1)) == 0);
        }
        return false;
    }

    /**
     * Returns {@code true} if the given expression is a literal expression that evaluates to 0.
     */
    private static boolean isZero(Expression expression) {
        if (expression instanceof IntegerLiteral integerLiteral) {
            return integerLiteral.asLong() == 0;
        } else if (expression instanceof FloatLiteral floatLiteral) {
            return Double.compare(floatLiteral.asDouble(), 0.0) == 0;
        }
        return false;
    }

    /**
     * Returns {@code true} if the given expression is a literal expression that evaluates to 1.
     */
    private static boolean isOne(Expression expression) {
        if (expression instanceof IntegerLiteral integerLiteral) {
            return integerLiteral.asLong() == 1;
        } else if (expression instanceof FloatLiteral floatLiteral) {
            return Double.compare(floatLiteral.asDouble(), 1.0) == 0;
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

    private static boolean isFloatLiteral(Expression expression) {
        return expression instanceof FloatLiteral;
    }

    private static boolean isNumericLiteral(Expression expression) {
        return expression instanceof IntegerLiteral || expression instanceof FloatLiteral;
    }

    private static boolean isStringLiteral(Expression expression) {
        return expression instanceof StringLiteral;
    }
}
