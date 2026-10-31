/*
 * Copyright (C) 2026 Johan Dykstrom
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

package se.dykstrom.jcc.col.semantics;

import se.dykstrom.jcc.col.compiler.OpaqueConversionFunction;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.error.InvalidValueException;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.common.types.F32;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.I32;
import se.dykstrom.jcc.common.types.Opaque;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;

import static java.util.Map.entry;
import static se.dykstrom.jcc.col.compiler.ColSymbols.*;

/**
 * Evaluates a COL constant expression at compile time, with the semantics the same expression
 * has at runtime: integers wrap at their width, f32 arithmetic is done in single precision, and
 * floats follow IEEE 754. Where the runtime result is undefined - integer division by zero, or a
 * float cast to an integer it does not fit in - evaluation fails instead.
 * <p>
 * A constant expression is made of literals, constants, operators, the numeric casts, opaque
 * conversions, and if-expressions. The expression must have passed semantic analysis, so that
 * types are known and implicit widening is explicit. The value is a literal of the underlying
 * type: an opaque conversion is the identity on the value.
 * <p>
 * The evaluator depends only on the AST, the types, and the symbol table, so that it can serve a
 * COL expression optimizer as well as the semantics parser.
 */
public class ColConstantEvaluator {

    private static final Set<Function> CASTS = Set.of(
            BF_F32_F32, BF_F32_F64, BF_F32_I32, BF_F32_I64,
            BF_F64_F32, BF_F64_F64, BF_F64_I32, BF_F64_I64,
            BF_I32_F32, BF_I32_F64, BF_I32_I32, BF_I32_I64,
            BF_I64_F32, BF_I64_F64, BF_I64_I32, BF_I64_I64
    );

    private static final Map<Class<? extends BinaryExpression>, BinaryEvaluator> BINARY_EVALUATORS = Map.ofEntries(
            entry(AddExpression.class, ColConstantEvaluator::add),
            entry(SubExpression.class, (be, l, r) -> arithmetic(be, l, r, (a, b) -> a - b, (a, b) -> a - b)),
            entry(MulExpression.class, (be, l, r) -> arithmetic(be, l, r, (a, b) -> a * b, (a, b) -> a * b)),
            entry(DivExpression.class, (be, l, r) -> arithmetic(be, l, r, null, (a, b) -> a / b)),
            entry(IDivExpression.class, (be, l, r) -> division(be, l, r, (a, b) -> a / b)),
            entry(ModExpression.class, (be, l, r) -> division(be, l, r, (a, b) -> a % b)),
            entry(AndExpression.class, (be, l, r) -> arithmetic(be, l, r, (a, b) -> a & b, null)),
            entry(OrExpression.class, (be, l, r) -> arithmetic(be, l, r, (a, b) -> a | b, null)),
            entry(XorExpression.class, (be, l, r) -> arithmetic(be, l, r, (a, b) -> a ^ b, null)),
            entry(LogicalAndExpression.class, (be, l, r) -> bool(be, asBoolean(l) && asBoolean(r))),
            entry(LogicalOrExpression.class, (be, l, r) -> bool(be, asBoolean(l) || asBoolean(r))),
            entry(LogicalXorExpression.class, (be, l, r) -> bool(be, asBoolean(l) ^ asBoolean(r))),
            entry(EqualExpression.class, (be, l, r) -> bool(be, isEqual(l, r))),
            entry(NotEqualExpression.class, (be, l, r) -> bool(be, !isEqual(l, r))),
            entry(LessExpression.class, (be, l, r) -> compare(be, l, r, (a, b) -> a < b, (a, b) -> a < b)),
            entry(LessOrEqualExpression.class, (be, l, r) -> compare(be, l, r, (a, b) -> a <= b, (a, b) -> a <= b)),
            entry(GreaterExpression.class, (be, l, r) -> compare(be, l, r, (a, b) -> a > b, (a, b) -> a > b)),
            entry(GreaterOrEqualExpression.class, (be, l, r) -> compare(be, l, r, (a, b) -> a >= b, (a, b) -> a >= b))
    );

    private final SymbolTable symbols;

    public ColConstantEvaluator(final SymbolTable symbols) {
        this.symbols = symbols;
    }

    /**
     * Returns the value of the given expression as a literal of its underlying type. Returns an
     * empty optional if the expression references a constant without a value, that is, one whose
     * own initializer failed and has already been reported. It also returns an empty optional if
     * an operand has the wrong type, which semantic analysis has already reported, as in -true.
     *
     * @throws NonConstantException If the expression is not a constant expression.
     * @throws InvalidValueException If the expression is constant, but its value is undefined.
     */
    public Optional<LiteralExpression> evaluate(final Expression expression) {
        try {
            return Optional.of(value(expression));
        } catch (FailedConstantException _) {
            return Optional.empty();
        }
    }

    private LiteralExpression value(final Expression expression) {
        return switch (expression) {
            case LiteralExpression le -> le;
            case IdentifierDerefExpression ide -> constant(ide);
            case UnaryExpression ue -> unary(ue, value(ue.getExpression()));
            case BinaryExpression be -> binary(be, value(be.getLeft()), value(be.getRight()));
            case IfExpression ie -> ifExpression(ie);
            case FunctionCallExpression fce -> functionCall(fce);
            default -> throw new NonConstantException(expression);
        };
    }

    private static LiteralExpression unary(final UnaryExpression ue, final LiteralExpression operand) {
        return switch (ue) {
            case CastToIntExpression ce -> castToInt(ce, operand, ce.type());
            case CastToFloatExpression ce -> castToFloat(ce, operand, ce.type());
            case NegateExpression ne -> negate(ne, operand);
            case NotExpression ne -> integer(ne, ~asLong(operand), operand.type());
            case LogicalNotExpression lne -> bool(lne, !asBoolean(operand));
            default -> throw new NonConstantException(ue);
        };
    }

    private LiteralExpression constant(final IdentifierDerefExpression ide) {
        final var name = ide.getIdentifier().name();
        if (!symbols.contains(name) || !symbols.isConstant(name)) {
            throw new NonConstantException(ide);
        }
        final var value = (String) symbols.getValue(name);
        if (value == null) {
            throw new FailedConstantException();
        }
        final var type = Opaque.unwrap(symbols.getType(name));
        return switch (type) {
            case Bool _ -> bool(ide, !BooleanLiteral.FALSE.getValue().equals(value));
            case Str _ -> new StringLiteral(ide.line(), ide.column(), value);
            case F32 _, F64 _ -> new FloatLiteral(ide.line(), ide.column(), value, type);
            default -> new IntegerLiteral(ide.line(), ide.column(), value, type);
        };
    }

    private LiteralExpression functionCall(final FunctionCallExpression fce) {
        final var function = fce.function();
        if (function instanceof OpaqueConversionFunction) {
            return value(fce.getArgs().getFirst());
        }
        if (CASTS.contains(function)) {
            final var operand = value(fce.getArgs().getFirst());
            final var target = function.getReturnType();
            return target.isInteger() ? castToInt(fce, operand, target) : castToFloat(fce, operand, target);
        }
        throw new NonConstantException(fce);
    }

    private LiteralExpression ifExpression(final IfExpression ie) {
        if (ie.elseExpr() == null) {
            throw new NonConstantException(ie);
        }
        final var condition = asBoolean(value(ie.ifExpr()));
        final var taken = condition ? ie.thenExpr() : ie.elseExpr();
        final var other = condition ? ie.elseExpr() : ie.thenExpr();
        // The branch not taken must be constant too, but its value does not matter, so a
        // division by zero there is not an error: if B == 0 then 0 else A div B
        try {
            value(other);
        } catch (InvalidValueException _) {
            // Ignored, see above
        }
        return value(taken);
    }

    private static LiteralExpression castToInt(final Node node, final LiteralExpression operand, final Type target) {
        if (operand instanceof IntegerLiteral il) {
            return integer(node, il.asLong(), target);
        }
        final var value = asDouble(operand);
        // LLVM's fptosi truncates toward zero, and gives poison if the result does not fit
        final var fits = (target instanceof I32)
                ? value > -2147483649.0 && value < 2147483648.0
                : value >= -9.223372036854775808E18 && value < 9.223372036854775808E18;
        if (!fits) {
            final var msg = "value " + value + " does not fit in " + target.getName().toLowerCase() + ": " + node;
            throw new InvalidValueException(msg, String.valueOf(value));
        }
        return integer(node, (long) value, target);
    }

    private static LiteralExpression castToFloat(final Node node, final LiteralExpression operand, final Type target) {
        if (operand instanceof IntegerLiteral il) {
            // Convert directly to the target precision, rounding only once
            final double value = (target instanceof F32) ? (float) il.asLong() : (double) il.asLong();
            return floating(node, value, target);
        }
        return floating(node, asDouble(operand), target);
    }

    private static LiteralExpression negate(final NegateExpression ne, final LiteralExpression operand) {
        if (operand instanceof IntegerLiteral il) {
            return integer(ne, -il.asLong(), il.type());
        }
        final var fl = asFloat(operand);
        return floating(ne, -asDouble(fl), fl.type());
    }

    private static LiteralExpression binary(final BinaryExpression be,
                                            final LiteralExpression left,
                                            final LiteralExpression right) {
        final var evaluator = BINARY_EVALUATORS.get(be.getClass());
        if (evaluator == null) {
            throw new NonConstantException(be);
        }
        return evaluator.evaluate(be, left, right);
    }

    private static LiteralExpression add(final BinaryExpression be,
                                         final LiteralExpression left,
                                         final LiteralExpression right) {
        if (left instanceof StringLiteral) {
            return new StringLiteral(be.line(), be.column(), left.getValue() + right.getValue());
        }
        return arithmetic(be, left, right, Long::sum, Double::sum);
    }

    private static LiteralExpression arithmetic(final BinaryExpression be,
                                                final LiteralExpression left,
                                                final LiteralExpression right,
                                                final LongBinaryOperator integerOperator,
                                                final DoubleBinaryOperator floatOperator) {
        if (left instanceof IntegerLiteral il) {
            return integer(be, integerOperator.applyAsLong(il.asLong(), asLong(right)), il.type());
        }
        final var fl = asFloat(left);
        // Two single precision operands give the correctly rounded single precision result when
        // the operation is done in double precision and then rounded
        return floating(be, floatOperator.applyAsDouble(asDouble(fl), asDouble(right)), fl.type());
    }

    private static LiteralExpression division(final BinaryExpression be,
                                              final LiteralExpression left,
                                              final LiteralExpression right,
                                              final LongBinaryOperator operator) {
        final var il = asInteger(left);
        final var divisor = asLong(right);
        if (divisor == 0) {
            final var msg = "division by zero: " + be;
            throw new InvalidValueException(msg, right.getValue());
        }
        final var min = (il.type() instanceof I32) ? Integer.MIN_VALUE : Long.MIN_VALUE;
        if (il.asLong() == min && divisor == -1) {
            final var msg = "integer overflow: " + be;
            throw new InvalidValueException(msg, il.getValue());
        }
        return integer(be, operator.applyAsLong(il.asLong(), divisor), il.type());
    }

    private static boolean isEqual(final LiteralExpression left, final LiteralExpression right) {
        return switch (left) {
            case IntegerLiteral il -> il.asLong() == asLong(right);
            case FloatLiteral _ -> asDouble(left) == asDouble(right);
            case BooleanLiteral _ -> asBoolean(left) == asBoolean(right);
            default -> left.getValue().equals(right.getValue());
        };
    }

    /**
     * Compares the operands with primitive operators, which for floats have the semantics of
     * LLVM's ordered predicates: every comparison with NaN is false.
     */
    private static BooleanLiteral compare(final BinaryExpression be,
                                          final LiteralExpression left,
                                          final LiteralExpression right,
                                          final LongComparison integerComparison,
                                          final DoubleComparison floatComparison) {
        if (left instanceof IntegerLiteral il) {
            return bool(be, integerComparison.test(il.asLong(), asLong(right)));
        }
        return bool(be, floatComparison.test(asDouble(left), asDouble(right)));
    }

    private static IntegerLiteral integer(final Node node, final long value, final Type type) {
        final long wrapped = (type instanceof I32) ? (int) value : value;
        return new IntegerLiteral(node.line(), node.column(), Long.toString(wrapped), type);
    }

    private static FloatLiteral floating(final Node node, final double value, final Type type) {
        final var text = (type instanceof F32) ? Float.toString((float) value) : Double.toString(value);
        return new FloatLiteral(node.line(), node.column(), text, type);
    }

    private static BooleanLiteral bool(final Node node, final boolean value) {
        return BooleanLiteral.from(node.line(), node.column(), Boolean.toString(value));
    }

    private static boolean asBoolean(final LiteralExpression literal) {
        if (literal instanceof BooleanLiteral bl) {
            return !BooleanLiteral.FALSE.getValue().equals(bl.getValue());
        }
        throw new FailedConstantException();
    }

    private static IntegerLiteral asInteger(final LiteralExpression literal) {
        if (literal instanceof IntegerLiteral il) {
            return il;
        }
        throw new FailedConstantException();
    }

    private static FloatLiteral asFloat(final LiteralExpression literal) {
        if (literal instanceof FloatLiteral fl) {
            return fl;
        }
        throw new FailedConstantException();
    }

    private static long asLong(final LiteralExpression literal) {
        return asInteger(literal).asLong();
    }

    /**
     * Returns the value of a float literal. A single precision literal is read as a float, because
     * its text is a decimal number, which is not the float value: 0.1f32 is 0.10000000149011612.
     */
    private static double asDouble(final LiteralExpression literal) {
        final var fl = asFloat(literal);
        return (fl.type() instanceof F32) ? Float.parseFloat(fl.getValue()) : fl.asDouble();
    }

    @FunctionalInterface
    private interface BinaryEvaluator {
        LiteralExpression evaluate(BinaryExpression expression, LiteralExpression left, LiteralExpression right);
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
     * Thrown when an expression that must be constant is not.
     */
    public static class NonConstantException extends SemanticsException {
        public NonConstantException(final Expression expression) {
            super("non-constant expression: " + expression);
        }
    }

    /**
     * Thrown when an expression references a constant whose initializer failed, or has an
     * operand of the wrong type. Either has already been reported.
     */
    private static class FailedConstantException extends RuntimeException {
        FailedConstantException() {
            super(null, null, false, false);
        }
    }
}
