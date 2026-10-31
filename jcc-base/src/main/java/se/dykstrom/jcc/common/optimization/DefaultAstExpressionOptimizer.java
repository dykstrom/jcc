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
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.common.types.F32;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.IntegerType;
import se.dykstrom.jcc.common.types.Str;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.common.utils.ExpressionUtils.hasNoFunctionCall;

/**
 * The default expression optimizer that performs AST optimizations applicable for all programming languages.
 * It optimizes an expression bottom-up, and folds each node with the component registered for its exact class.
 * A language can replace a built-in component, or add components of its own.
 *
 * @author Johan Dykstrom
 */
public class DefaultAstExpressionOptimizer implements AstExpressionOptimizer {

    /** Maps exact expression class to component. */
    private final Map<Class<? extends Expression>, ExpressionComponent<?>> components = new HashMap<>();

    private final TypeManager typeManager;

    public DefaultAstExpressionOptimizer(final TypeManager typeManager) {
        this(typeManager, List.of());
    }

    /**
     * Creates an optimizer with the built-in components and the given components. A given
     * component replaces the built-in component for the same expression class.
     */
    public DefaultAstExpressionOptimizer(final TypeManager typeManager, final List<ExpressionComponent<?>> components) {
        this.typeManager = requireNonNull(typeManager);
        addBuiltIn(AddExpression.class, this::addExpression);
        addBuiltIn(SubExpression.class, this::subExpression);
        addBuiltIn(MulExpression.class, this::mulExpression);
        addBuiltIn(DivExpression.class, this::divExpression);
        addBuiltIn(IDivExpression.class, this::idivExpression);
        addBuiltIn(ModExpression.class, this::modExpression);
        addBuiltIn(AndExpression.class, this::andExpression);
        addBuiltIn(OrExpression.class, DefaultAstExpressionOptimizer::orExpression);
        addBuiltIn(XorExpression.class, DefaultAstExpressionOptimizer::xorExpression);
        addBuiltIn(EqualExpression.class, this::eqExpression);
        addBuiltIn(NotEqualExpression.class, this::neExpression);
        addBuiltIn(LessExpression.class, this::ltExpression);
        addBuiltIn(LessOrEqualExpression.class, this::leExpression);
        addBuiltIn(GreaterExpression.class, this::gtExpression);
        addBuiltIn(GreaterOrEqualExpression.class, this::geExpression);
        addBuiltIn(CastToFloatExpression.class, DefaultAstExpressionOptimizer::castToFloatExpression);
        addBuiltIn(CastToIntExpression.class, DefaultAstExpressionOptimizer::castToIntExpression);
        addBuiltIn(NegateExpression.class, DefaultAstExpressionOptimizer::negateExpression);
        addBuiltIn(NotExpression.class, DefaultAstExpressionOptimizer::notExpression);
        add(new ExpressionComponent<>(IdentifierDerefExpression.class, (e, symbols, _) -> constant(e, symbols)));
        addBuiltIn(IfExpression.class, DefaultAstExpressionOptimizer::ifExpression);
        components.forEach(this::add);
    }

    private void add(final ExpressionComponent<?> component) {
        this.components.put(component.type(), component);
    }

    private <T extends Expression> void addBuiltIn(final Class<T> type, final Function<T, Expression> optimizer) {
        add(new ExpressionComponent<>(type, (e, _, _) -> optimizer.apply(e)));
    }

    @Override
    public Expression expression(final Expression expression, final SymbolTable symbols) {
        final Expression optimized = switch (expression) {
            case UnaryExpression ue -> ue.withExpression(expression(ue.getExpression(), symbols));
            case BinaryExpression be -> be.withLeft(expression(be.getLeft(), symbols))
                                          .withRight(expression(be.getRight(), symbols));
            case FunctionCallExpression fce -> fce.withArgs(expressions(fce.getArgs(), symbols));
            case IfExpression ie -> ie.withIfExpr(expression(ie.ifExpr(), symbols))
                                      .withThenExpr(expression(ie.thenExpr(), symbols))
                                      .withElseExpr(expression(ie.elseExpr(), symbols));
            default -> expression;
        };
        final var component = components.get(optimized.getClass());
        return (component != null) ? component.apply(optimized, symbols, this) : optimized;
    }

    /**
     * Replaces an if-expression whose condition is a boolean literal with the branch the
     * condition selects.
     */
    private static Expression ifExpression(final IfExpression ie) {
        if (ie.ifExpr() instanceof BooleanLiteral condition) {
            return condition.getValue().equals(BooleanLiteral.TRUE.getValue()) ? ie.thenExpr() : ie.elseExpr();
        }
        return ie;
    }

    /**
     * Replaces a reference to a constant with the constant's value.
     */
    private static Expression constant(final IdentifierDerefExpression ide, final SymbolTable symbols) {
        final var name = ide.getIdentifier().name();
        if (symbols.contains(name) && symbols.isConstant(name)) {
            final var line = ide.line();
            final var column = ide.column();
            final var type = ide.getIdentifier().type();
            final var value = symbols.getValue(name);

            if (type.isInteger()) {
                return new IntegerLiteral(line, column, (String) value, type);
            } else if (type.isFloat()) {
                return new FloatLiteral(line, column, (String) value, type);
            } else if (type instanceof Str) {
                return new StringLiteral(line, column, (String) value);
            }
        }
        return ide;
    }

    private static Expression castToFloatExpression(final CastToFloatExpression ce) {
        final var type = ce.type();
        if (ce.getExpression() instanceof FloatLiteral f) {
            if (type instanceof F64) {
                return new FloatLiteral(ce.line(), ce.column(), f.asDouble(), type);
            } else { // F32
                return new FloatLiteral(ce.line(), ce.column(), (float) f.asDouble(), type);
            }
        } else if (ce.getExpression() instanceof IntegerLiteral i) {
            if (type instanceof F64) {
                return new FloatLiteral(ce.line(), ce.column(), (double) i.asLong(), type);
            } else { // F32
                return new FloatLiteral(ce.line(), ce.column(), (float) i.asLong(), type);
            }
        }
        return ce;
    }

    private static Expression castToIntExpression(final CastToIntExpression ce) {
        final var type = ce.type();
        if (ce.getExpression() instanceof IntegerLiteral i) {
            if (type instanceof I64) {
                return new IntegerLiteral(ce.line(), ce.column(), i.asLong(), type);
            } else { // I32
                return new IntegerLiteral(ce.line(), ce.column(), (int) i.asLong(), type);
            }
        } else if (ce.getExpression() instanceof FloatLiteral f) {
            if (type instanceof I64) {
                return new IntegerLiteral(ce.line(), ce.column(), (long) f.asDouble(), type);
            } else { // I32
                return new IntegerLiteral(ce.line(), ce.column(), (int) f.asDouble(), type);
            }
        }
        return ce;
    }

    /**
     * Simplifies the given negate expression to a literal expression if possible.
     */
    private static Expression negateExpression(final NegateExpression ne) {
        final var expr = ne.getExpression();
        if (isIntegerLiteral(expr)) {
            return integerLiteral(ne, -asLong(expr), expr);
        } else if (isNumericLiteral(expr)) {
            return floatLiteral(ne, -asDouble(expr), expr);
        } else if (expr instanceof NegateExpression inner) {
            return inner.getExpression();
        } else if (expr instanceof SubExpression subExpr && hasNoFunctionCall(subExpr)) {
            // If the expression includes a function call, we cannot change
            // the order of left and right. BASIC functions are impure, and
            // may change global state.
            return new SubExpression(subExpr.line(), subExpr.column(), subExpr.getRight(), subExpr.getLeft());
        }
        return ne;
    }

    /**
     * Simplifies a NOT expression to a literal expression if possible.
     */
    private static Expression notExpression(final NotExpression ne) {
        final var expr = ne.getExpression();
        if (isIntegerLiteral(expr)) {
            return integerLiteral(ne, ~asLong(expr), expr);
        } else if (expr instanceof NotExpression inner) {
            return inner.getExpression();
        }
        return ne;
    }

    /**
     * Simplifies an add expression to a literal expression if possible.
     */
    private Expression addExpression(final AddExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        if (isIntegerZeroIdentity(left, right, e)) {
            return right;
        } else if (isIntegerZeroIdentity(right, left, e)) {
            return left;
        } else if (isIntegerLiteral(left) && isIntegerLiteral(right)) {
            return integerLiteral(e, asLong(left) + asLong(right), left, right);
        } else if (isNumericLiteral(left) && isNumericLiteral(right)) {
            return floatLiteral(e, asDouble(left) + asDouble(right), left, right);
        } else if (isStringLiteral(left) && isStringLiteral(right)) {
            return new StringLiteral(e.line(), e.column(), asString(left) + asString(right));
        }
        return e;
    }

    /**
     * Simplifies a sub expression to a literal expression if possible.
     */
    private Expression subExpression(final SubExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        // Subtracting zero is an IEEE 754 identity also for floats (x - 0.0 == x for -0.0 and NaN),
        // so the fold only needs to preserve the expression type
        if (isZero(right) && sameTypeAs(left, e)) {
            return left;
        } else if (isIntegerLiteral(left) && isIntegerLiteral(right)) {
            return integerLiteral(e, asLong(left) - asLong(right), left, right);
        } else if (isNumericLiteral(left) && isNumericLiteral(right)) {
            return floatLiteral(e, asDouble(left) - asDouble(right), left, right);
        }
        return e;
    }

    /**
     * Simplifies a mul expression to a literal expression if possible. Otherwise, tries to replace
     * the multiplication with a shift expression if that is possible.
     */
    private Expression mulExpression(final MulExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        if (isIntegerZeroFold(left, right, e) || isIntegerZeroFold(right, left, e)) {
            return zero(e);
        } else if (isOne(left) && sameTypeAs(right, e)) {
            return right;
        } else if (isOne(right) && sameTypeAs(left, e)) {
            return left;
        }
        return mulLiteralOrShiftExpression(e);
    }

    /**
     * Simplifies a mul expression by folding literals, or by replacing the
     * multiplication with a shift expression if one factor is a power of two.
     */
    private Expression mulLiteralOrShiftExpression(final MulExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        if (isIntegerLiteral(left) && isIntegerLiteral(right)) {
            return integerLiteral(e, asLong(left) * asLong(right), left, right);
        } else if (isNumericLiteral(left) && isNumericLiteral(right)) {
            return floatLiteral(e, asDouble(left) * asDouble(right), left, right);
        } else if (isPowerOfTwo(left) && isIntegerType(right)) {
            return new ShiftLeftExpression(e.line(), e.column(), right, shiftAmount(e, left));
        } else if (isIntegerType(left) && isPowerOfTwo(right)) {
            return new ShiftLeftExpression(e.line(), e.column(), left, shiftAmount(e, right));
        }
        return e;
    }

    /**
     * Simplifies a div expression to a literal expression if possible.
     */
    private Expression divExpression(final DivExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        // Folding 0.0 / x to 0.0 would violate IEEE 754: the result is -0.0 for negative x and NaN for NaN
        if (isZero(right)) {
            throw new InvalidValueException("division by zero: " + right, right.toString());
        } else if (isNumericLiteral(left) && isNumericLiteral(right)) {
            final var result = floatLiteral(e, asDouble(left) / asDouble(right), left, right);
            // Do not fold an overflowing division into an inf literal
            if (Double.isFinite(result.asDouble())) {
                return result;
            }
        } else if (isOne(right) && sameTypeAs(left, e)) {
            // Dividing by one is an IEEE 754 identity, but an integer divided by a float is a float,
            // so the fold must not replace the division with an integer operand
            return left;
        }
        return e;
    }

    /**
     * Simplifies an idiv (integer division) expression to a literal expression if possible.
     */
    private Expression idivExpression(final IDivExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        if (isZero(left) && hasNoFunctionCall(right)) {
            return zero(e);
        } else if (isZero(right)) {
            throw new InvalidValueException("division by zero: " + right, right.toString());
        } else if (isOne(right)) {
            return left;
        } else if (isIntegerLiteral(left) && isIntegerLiteral(right) && !isOverflowingDivision(left, right)) {
            return integerLiteral(e, asLong(left) / asLong(right), left, right);
        }
        return e;
    }

    /**
     * Simplifies a MOD expression to a literal expression if possible.
     */
    private Expression modExpression(final ModExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        if (isZero(left) && hasNoFunctionCall(right)) {
            return zero(e);
        } else if (isZero(right)) {
            throw new InvalidValueException("division by zero: " + right, right.toString());
        } else if (isOne(right)) {
            return zero(e);
        } else if (isIntegerLiteral(left) && isIntegerLiteral(right) && !isOverflowingDivision(left, right)) {
            return integerLiteral(e, asLong(left) % asLong(right), left, right);
        }
        return e;
    }

    /**
     * Simplifies an AND expression to a literal expression if possible.
     */
    private Expression andExpression(final AndExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        if ((isZero(left) && hasNoFunctionCall(right)) || (isZero(right) && hasNoFunctionCall(left))) {
            return zero(e);
        } else if (isIntegerLiteral(left) && isIntegerLiteral(right)) {
            return integerLiteral(e, asLong(left) & asLong(right), left, right);
        }
        return e;
    }

    /**
     * Simplifies an OR expression to a literal expression if possible.
     */
    private static Expression orExpression(final OrExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        if (isZero(left)) {
            return right;
        } else if (isZero(right)) {
            return left;
        } else if (isIntegerLiteral(left) && isIntegerLiteral(right)) {
            return integerLiteral(e, asLong(left) | asLong(right), left, right);
        }
        return e;
    }

    /**
     * Simplifies an XOR expression to a literal expression if possible.
     */
    private static Expression xorExpression(final XorExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        if (isZero(left)) {
            return right;
        } else if (isZero(right)) {
            return left;
        } else if (isIntegerLiteral(left) && isIntegerLiteral(right)) {
            return integerLiteral(e, asLong(left) ^ asLong(right), left, right);
        }
        return e;
    }

    /**
     * Simplifies an EQ expression to a literal expression if possible.
     */
    private Expression eqExpression(final EqualExpression e) {
        return relational(e, (a, b) -> a == b, (a, b) -> a == b);
    }

    /**
     * Simplifies an NE expression to a literal expression if possible.
     */
    private Expression neExpression(final NotEqualExpression e) {
        return relational(e, (a, b) -> a != b, (a, b) -> a != b);
    }

    /**
     * Simplifies an LT expression to a literal expression if possible.
     */
    private Expression ltExpression(final LessExpression e) {
        return relational(e, (a, b) -> a < b, (a, b) -> a < b);
    }

    /**
     * Simplifies an LE expression to a literal expression if possible.
     */
    private Expression leExpression(final LessOrEqualExpression e) {
        return relational(e, (a, b) -> a <= b, (a, b) -> a <= b);
    }

    /**
     * Simplifies a GT expression to a literal expression if possible.
     */
    private Expression gtExpression(final GreaterExpression e) {
        return relational(e, (a, b) -> a > b, (a, b) -> a > b);
    }

    /**
     * Simplifies a GE expression to a literal expression if possible.
     */
    private Expression geExpression(final GreaterOrEqualExpression e) {
        return relational(e, (a, b) -> a >= b, (a, b) -> a >= b);
    }

    /**
     * Folds a relational expression with two literal operands into a literal of the expression's
     * type: a boolean literal if the language types a comparison as Bool, and otherwise the
     * integer -1 for true and 0 for false. Floats are compared with the primitive operators,
     * which follow IEEE 754: every comparison with NaN is false except !=, and -0.0 equals 0.0.
     * Returns the expression unchanged if the operands are not both literals.
     */
    private Expression relational(final BinaryExpression expression,
                                  final LongComparison integerComparison,
                                  final DoubleComparison floatComparison) {
        final var left = expression.getLeft();
        final var right = expression.getRight();
        final boolean result;
        if (isIntegerLiteral(left) && isIntegerLiteral(right)) {
            result = integerComparison.test(asLong(left), asLong(right));
        } else if (isNumericLiteral(left) && isNumericLiteral(right)) {
            result = floatComparison.test(asDouble(left), asDouble(right));
        } else if (isStringLiteral(left) && isStringLiteral(right)) {
            result = integerComparison.test(asString(left).compareTo(asString(right)), 0);
        } else {
            return expression;
        }
        if (typeManager.getType(expression) instanceof Bool) {
            return BooleanLiteral.from(expression.line(), expression.column(), Boolean.toString(result));
        }
        return new IntegerLiteral(expression.line(), expression.column(), result ? -1L : 0L);
    }

    /**
     * An expression optimizer, and the exact class of expression it optimizes.
     */
    public record ExpressionComponent<T extends Expression>(Class<T> type, ExpressionOptimizer<T> optimizer) {

        public ExpressionComponent {
            requireNonNull(type);
            requireNonNull(optimizer);
        }

        private Expression apply(final Expression expression,
                                 final SymbolTable symbols,
                                 final AstExpressionOptimizer astOptimizer) {
            return optimizer.optimize(type.cast(expression), symbols, astOptimizer);
        }
    }

    @FunctionalInterface
    private interface LongComparison {
        boolean test(long left, long right);
    }

    @FunctionalInterface
    private interface DoubleComparison {
        boolean test(double left, double right);
    }

    /**
     * Returns the given expression, which has to be a literal expression, as a long.
     */
    private static long asLong(Expression expression) {
        return Long.parseLong(((LiteralExpression) expression).getValue());
    }

    /**
     * Returns the given expression, which has to be a literal expression, as a double. A single
     * precision literal is read as a float, because its text is a decimal number, which is not
     * the float value: 0.1 as an F32 is 0.10000000149011612.
     */
    private static double asDouble(Expression expression) {
        if (expression instanceof FloatLiteral fl && fl.type() instanceof F32) {
            return Float.parseFloat(fl.getValue());
        }
        return Double.parseDouble(((LiteralExpression) expression).getValue());
    }

    /**
     * Returns an integer literal at the position of the given node, with the result of folding the
     * given literal operands. The result has the type {@link #integerType} returns, and wraps
     * around at its width, as it does at runtime.
     */
    private static IntegerLiteral integerLiteral(final Node node, final long value, final Expression... operands) {
        final var type = integerType(operands);
        if (type.bits() < 64) {
            final var shift = 64 - type.bits();
            return new IntegerLiteral(node.line(), node.column(), (value << shift) >> shift, type);
        }
        return new IntegerLiteral(node.line(), node.column(), value, type);
    }

    /**
     * Returns the type of an integer literal folded from the given literal operands: their type
     * if every operand has the same integer type, and otherwise I64.
     */
    private static IntegerType integerType(final Expression... operands) {
        final var type = ((LiteralExpression) operands[0]).type();
        final var sameType = Arrays.stream(operands).allMatch(o -> type.equals(((LiteralExpression) o).type()));
        return (sameType && type instanceof IntegerType it) ? it : I64.INSTANCE;
    }

    /**
     * Returns true if dividing the given integer literals overflows: the smallest value of the
     * result type divided by -1. The quotient and remainder of that division are undefined at
     * runtime, so the division is not folded.
     */
    private static boolean isOverflowingDivision(final Expression left, final Expression right) {
        final var bits = integerType(left, right).bits();
        final var min = (bits == 64) ? Long.MIN_VALUE : -(1L << (bits - 1));
        return asLong(left) == min && asLong(right) == -1;
    }

    /**
     * Returns a zero literal of the type of the given expression, which folds to zero whatever
     * the value of its non-literal operand.
     */
    private IntegerLiteral zero(final BinaryExpression expression) {
        return new IntegerLiteral(expression.line(), expression.column(), 0L, typeManager.getType(expression));
    }

    /**
     * Returns a float literal at the position of the given node, with the result of folding the given literal operands. The result
     * is F32 only if every operand is an F32 literal, and it is then rounded to single precision,
     * so the literal holds the value the operation has at runtime. For two single precision
     * operands, an operation done in double precision and then rounded gives the correctly
     * rounded single precision result. Otherwise, the result is F64.
     */
    private static FloatLiteral floatLiteral(final Node node, final double value, final Expression... operands) {
        final var single = Arrays.stream(operands)
                                 .allMatch(o -> o instanceof FloatLiteral fl && fl.type() instanceof F32);
        if (single) {
            return new FloatLiteral(node.line(), node.column(), (float) value, F32.INSTANCE);
        }
        return new FloatLiteral(node.line(), node.column(), value);
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
     * Returns the shift amount that replaces multiplication by the given power of two. It has the
     * type of the power of two, so that the shift has the type the multiplication had.
     */
    private IntegerLiteral shiftAmount(final Node node, final Expression powerOfTwo) {
        return new IntegerLiteral(node.line(), node.column(), asShift(powerOfTwo), ((IntegerLiteral) powerOfTwo).type());
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
        return typeManager.getType(expression).isInteger();
    }

    /**
     * Returns {@code true} if the given operand has the same static type as the given expression,
     * so that the expression can be replaced by the operand without changing its type.
     */
    private boolean sameTypeAs(Expression operand, Expression expression) {
        return typeManager.getType(operand).equals(typeManager.getType(expression));
    }

    /**
     * Returns {@code true} if {@code zero} is a zero literal and the whole expression is integer-typed,
     * so that the expression can be folded to {@code operand}. For floats, this fold would violate
     * IEEE 754: 0.0 + -0.0 is +0.0.
     */
    private boolean isIntegerZeroIdentity(Expression zero, Expression operand, Expression expression) {
        return isZero(zero) && isIntegerType(expression) && sameTypeAs(operand, expression);
    }

    /**
     * Returns {@code true} if {@code zero} is a zero literal and the whole expression is integer-typed,
     * so that the expression can be folded to a zero literal. For floats, this fold would violate
     * IEEE 754: 0.0 * inf is NaN, and 0.0 * -1.0 is -0.0. The discarded operand must not contain
     * a function call, because functions may have side effects.
     */
    private boolean isIntegerZeroFold(Expression zero, Expression discarded, Expression expression) {
        return isZero(zero) && hasNoFunctionCall(discarded) && isIntegerType(expression);
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
