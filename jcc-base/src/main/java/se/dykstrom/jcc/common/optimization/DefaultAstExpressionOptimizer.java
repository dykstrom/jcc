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
import se.dykstrom.jcc.common.evaluation.ConstantEvaluator;
import se.dykstrom.jcc.common.evaluation.ConstantEvaluator.CallEvaluator;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.common.types.Str;

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
 * <p>
 * A built-in component first asks the {@link ConstantEvaluator} for the value of a node whose operands are
 * all literals. The optimizer does no arithmetic of its own: it only rewrites what the evaluator cannot fold,
 * such as {@code x + 0}, and it leaves a node whose value is undefined, such as {@code 1 \ 0}, unchanged.
 *
 * @author Johan Dykstrom
 */
public class DefaultAstExpressionOptimizer implements AstExpressionOptimizer {

    /** Maps exact expression class to component. */
    private final Map<Class<? extends Expression>, ExpressionComponent<?>> components = new HashMap<>();

    private final TypeManager typeManager;
    private final ConstantEvaluator evaluator = new ConstantEvaluator(CallEvaluator.NONE, this::booleanResult);

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
        addBuiltIn(EqualExpression.class, e -> e);
        addBuiltIn(NotEqualExpression.class, e -> e);
        addBuiltIn(LessExpression.class, e -> e);
        addBuiltIn(LessOrEqualExpression.class, e -> e);
        addBuiltIn(GreaterExpression.class, e -> e);
        addBuiltIn(GreaterOrEqualExpression.class, e -> e);
        addBuiltIn(CastToFloatExpression.class, e -> e);
        addBuiltIn(CastToIntExpression.class, e -> e);
        addBuiltIn(NegateExpression.class, DefaultAstExpressionOptimizer::negateExpression);
        addBuiltIn(NotExpression.class, DefaultAstExpressionOptimizer::notExpression);
        add(new ExpressionComponent<>(IdentifierDerefExpression.class, (e, symbols, _) -> constant(e, symbols)));
        addBuiltIn(IfExpression.class, DefaultAstExpressionOptimizer::ifExpression);
        components.forEach(this::add);
    }

    private void add(final ExpressionComponent<?> component) {
        this.components.put(component.type(), component);
    }

    /**
     * Adds a built-in component that folds a node with the evaluator if it can, and otherwise
     * rewrites it with {@code optimizer}.
     */
    private <T extends Expression> void addBuiltIn(final Class<T> type, final Function<T, Expression> optimizer) {
        add(new ExpressionComponent<>(type, (e, _, _) -> evaluator.fold(e)
                                                                  .<Expression>map(literal -> literal)
                                                                  .orElseGet(() -> optimizer.apply(e))));
    }

    /**
     * Returns the literal that holds a boolean result: a boolean literal if the language types
     * the expression as Bool, and otherwise the integer -1 for true and 0 for false.
     */
    private LiteralExpression booleanResult(final Node node, final boolean value) {
        if (typeManager.getType((Expression) node) instanceof Bool) {
            return ConstantEvaluator.booleanLiteral(node, value);
        }
        return ConstantEvaluator.integerBoolean(node, value);
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

    /**
     * Simplifies a negate expression: -(-x) is x, and -(a - b) is b - a.
     */
    private static Expression negateExpression(final NegateExpression ne) {
        final var expr = ne.getExpression();
        if (expr instanceof NegateExpression inner) {
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
     * Simplifies a NOT expression: NOT NOT x is x.
     */
    private static Expression notExpression(final NotExpression ne) {
        final var expr = ne.getExpression();
        if (expr instanceof NotExpression inner) {
            return inner.getExpression();
        }
        return ne;
    }

    /**
     * Simplifies an add expression: x + 0 is x for integers.
     */
    private Expression addExpression(final AddExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        if (isIntegerZeroIdentity(left, right, e)) {
            return right;
        } else if (isIntegerZeroIdentity(right, left, e)) {
            return left;
        }
        return e;
    }

    /**
     * Simplifies a sub expression: x - 0 is x.
     */
    private Expression subExpression(final SubExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        // Subtracting zero is an IEEE 754 identity also for floats (x - 0.0 == x for -0.0 and NaN),
        // so the fold only needs to preserve the expression type
        if (isZero(right) && sameTypeAs(left, e)) {
            return left;
        }
        return e;
    }

    /**
     * Simplifies a mul expression: 0 * x is 0 for integers, and 1 * x is x. Otherwise, tries to
     * replace the multiplication with a shift expression.
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
        return shiftExpression(e);
    }

    /**
     * Replaces a mul expression with a shift expression if one factor is a power of two.
     */
    private Expression shiftExpression(final MulExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        if (isPowerOfTwo(left) && isIntegerType(right)) {
            return new ShiftLeftExpression(e.line(), e.column(), right, shiftAmount(e, left));
        } else if (isIntegerType(left) && isPowerOfTwo(right)) {
            return new ShiftLeftExpression(e.line(), e.column(), left, shiftAmount(e, right));
        }
        return e;
    }

    /**
     * Simplifies a div expression: x / 1 is x.
     */
    private Expression divExpression(final DivExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        // Folding 0.0 / x to 0.0 would violate IEEE 754: the result is -0.0 for negative x and NaN for NaN
        if (isOne(right) && sameTypeAs(left, e)) {
            // Dividing by one is an IEEE 754 identity, but an integer divided by a float is a float,
            // so the fold must not replace the division with an integer operand
            return left;
        }
        return e;
    }

    /**
     * Simplifies an idiv (integer division) expression: 0 \ x is 0, and x \ 1 is x.
     */
    private Expression idivExpression(final IDivExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        if (isZero(left) && hasNoFunctionCall(right)) {
            return zero(e);
        } else if (isOne(right)) {
            return left;
        }
        return e;
    }

    /**
     * Simplifies an integer MOD expression: 0 MOD x and x MOD 1 are 0. A float MOD is not
     * simplified, because x MOD 1.0 is the fractional part of x.
     */
    private Expression modExpression(final ModExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        if (isIntegerZeroFold(left, right, e)) {
            return zero(e);
        } else if (isOne(right) && hasNoFunctionCall(left) && isIntegerType(e)) {
            return zero(e);
        }
        return e;
    }

    /**
     * Simplifies an AND expression: 0 AND x is 0.
     */
    private Expression andExpression(final AndExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        if ((isZero(left) && hasNoFunctionCall(right)) || (isZero(right) && hasNoFunctionCall(left))) {
            return zero(e);
        }
        return e;
    }

    /**
     * Simplifies an OR expression: 0 OR x is x.
     */
    private static Expression orExpression(final OrExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        if (isZero(left)) {
            return right;
        } else if (isZero(right)) {
            return left;
        }
        return e;
    }

    /**
     * Simplifies an XOR expression: 0 XOR x is x.
     */
    private static Expression xorExpression(final XorExpression e) {
        final var left = e.getLeft();
        final var right = e.getRight();
        if (isZero(left)) {
            return right;
        } else if (isZero(right)) {
            return left;
        }
        return e;
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

    /**
     * Returns a zero literal of the type of the given expression, which folds to zero whatever
     * the value of its non-literal operand.
     */
    private IntegerLiteral zero(final BinaryExpression expression) {
        return new IntegerLiteral(expression.line(), expression.column(), 0L, typeManager.getType(expression));
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
}
