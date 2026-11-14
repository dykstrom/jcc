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

package se.dykstrom.jcc.common.evaluation;

import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.error.InvalidValueException;
import se.dykstrom.jcc.common.error.NonConstantException;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.common.types.F32;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.IntegerType;
import se.dykstrom.jcc.common.types.Opaque;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.entry;
import static java.util.Objects.requireNonNull;

/**
 * Evaluates a constant expression at compile time, with the semantics the same expression has
 * at runtime: integers wrap at their width, f32 arithmetic is done in single precision, and
 * floats follow IEEE 754. Where the runtime result is undefined - integer division by zero, or a
 * float cast to an integer it does not fit in - evaluation fails instead.
 * <p>
 * A constant expression is made of literals, constants, operators, the numeric casts, the
 * function calls the language says are constant, and if-expressions. The expression must have
 * passed semantic analysis, so that types are known and implicit widening is explicit.
 * <p>
 * A language configures the evaluator with two hooks: which function calls are constant and what
 * they evaluate to, and the literal that holds a boolean result, such as a comparison.
 */
public class ConstantEvaluator {

    /**
     * Evaluates the function calls a language treats as constant, such as casts written as calls.
     */
    public interface CallEvaluator {

        /** A call evaluator for a language in which no function call is constant. */
        CallEvaluator NONE = new CallEvaluator() {
            @Override
            public boolean isConstant(final Function function) {
                return false;
            }

            @Override
            public LiteralExpression evaluate(final FunctionCallExpression call, final List<LiteralExpression> args) {
                throw new NonConstantException(call);
            }
        };

        /**
         * Returns {@code true} if a call to {@code function} is constant when its arguments are.
         */
        boolean isConstant(Function function);

        /**
         * Returns the value of {@code call}, given the values of its arguments.
         */
        LiteralExpression evaluate(FunctionCallExpression call, List<LiteralExpression> args);
    }

    /**
     * Creates the literal that holds a boolean result, such as the result of a comparison.
     */
    @FunctionalInterface
    public interface BooleanResult {
        LiteralExpression literal(Node node, boolean value);
    }

    private final Map<Class<? extends BinaryExpression>, BinaryEvaluator> binaryEvaluators = Map.ofEntries(
            entry(AddExpression.class, ConstantEvaluator::add),
            entry(SubExpression.class, (be, l, r) -> arithmetic(be, l, r, BigInteger::subtract, (a, b) -> a - b)),
            entry(MulExpression.class, (be, l, r) -> arithmetic(be, l, r, BigInteger::multiply, (a, b) -> a * b)),
            entry(DivExpression.class, (be, l, r) -> arithmetic(be, l, r, null, (a, b) -> a / b)),
            entry(IDivExpression.class, (be, l, r) -> division(be, l, r, BigInteger::divide)),
            entry(ModExpression.class, (be, l, r) -> division(be, l, r, BigInteger::remainder)),
            entry(AndExpression.class, (be, l, r) -> arithmetic(be, l, r, BigInteger::and, null)),
            entry(OrExpression.class, (be, l, r) -> arithmetic(be, l, r, BigInteger::or, null)),
            entry(XorExpression.class, (be, l, r) -> arithmetic(be, l, r, BigInteger::xor, null)),
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

    private final CallEvaluator callEvaluator;
    private final BooleanResult booleanResult;

    public ConstantEvaluator(final CallEvaluator callEvaluator, final BooleanResult booleanResult) {
        this.callEvaluator = requireNonNull(callEvaluator);
        this.booleanResult = requireNonNull(booleanResult);
    }

    /**
     * Returns the value of the given expression as a literal of its underlying type, looking up
     * the constants it references in {@code symbols}. Returns an empty optional if the expression
     * references a constant without a value, that is, one whose own initializer failed and has
     * already been reported. It also returns an empty optional if an operand has the wrong type,
     * which semantic analysis has already reported, as in -true.
     *
     * @throws NonConstantException If the expression is not a constant expression.
     * @throws InvalidValueException If the expression is constant, but its value is undefined.
     */
    public Optional<LiteralExpression> evaluate(final Expression expression, final SymbolTable symbols) {
        try {
            return Optional.of(value(expression, symbols));
        } catch (FailedConstantException _) {
            return Optional.empty();
        }
    }

    /**
     * Returns the value of the given unary expression, binary expression, or function call, if all
     * its operands are literals. Constants are not looked up, so a reference to one is not folded.
     * Returns an empty optional if the expression cannot be folded: if an operand is not a literal,
     * if the expression is not constant, or if its value is undefined.
     */
    public Optional<LiteralExpression> fold(final Expression expression) {
        try {
            return switch (expression) {
                case UnaryExpression ue when ue.getExpression() instanceof LiteralExpression operand ->
                        Optional.of(unary(ue, operand));
                case BinaryExpression be when be.getLeft() instanceof LiteralExpression left && be.getRight() instanceof LiteralExpression right ->
                        Optional.of(binary(be, left, right));
                case FunctionCallExpression fce when fce.getArgs().stream().allMatch(LiteralExpression.class::isInstance) ->
                        Optional.of(functionCall(fce, arg -> (LiteralExpression) arg));
                default -> Optional.empty();
            };
        } catch (NonConstantException | InvalidValueException | FailedConstantException _) {
            return Optional.empty();
        }
    }

    private LiteralExpression value(final Expression expression, final SymbolTable symbols) {
        return switch (expression) {
            case LiteralExpression le -> le;
            case IdentifierDerefExpression ide -> constant(ide, symbols);
            case UnaryExpression ue -> unary(ue, value(ue.getExpression(), symbols));
            case BinaryExpression be -> binary(be, value(be.getLeft(), symbols), value(be.getRight(), symbols));
            case IfExpression ie -> ifExpression(ie, symbols);
            case FunctionCallExpression fce -> functionCall(fce, arg -> value(arg, symbols));
            default -> throw new NonConstantException(expression);
        };
    }

    private LiteralExpression unary(final UnaryExpression ue, final LiteralExpression operand) {
        return switch (ue) {
            case CastToIntExpression ce -> castToInt(ce, operand, ce.type());
            case CastToFloatExpression ce -> castToFloat(ce, operand, ce.type());
            case NegateExpression ne -> negate(ne, operand);
            case NotExpression ne -> integer(ne, asBigInteger(operand).not(), operand.type());
            case LogicalNotExpression lne -> bool(lne, !asBoolean(operand));
            default -> throw new NonConstantException(ue);
        };
    }

    private LiteralExpression constant(final IdentifierDerefExpression ide, final SymbolTable symbols) {
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

    private LiteralExpression functionCall(final FunctionCallExpression fce, final ArgValue argValue) {
        final var function = fce.function();
        if (function == null) {
            // A call that did not resolve has already been reported
            throw new FailedConstantException();
        }
        if (!callEvaluator.isConstant(function)) {
            throw new NonConstantException(fce);
        }
        final var args = fce.getArgs().stream().map(argValue::of).toList();
        return callEvaluator.evaluate(fce, args);
    }

    private LiteralExpression ifExpression(final IfExpression ie, final SymbolTable symbols) {
        if (ie.elseExpr() == null) {
            throw new NonConstantException(ie);
        }
        final var condition = asBoolean(value(ie.ifExpr(), symbols));
        final var taken = condition ? ie.thenExpr() : ie.elseExpr();
        final var other = condition ? ie.elseExpr() : ie.thenExpr();
        // The branch not taken must be constant too, but its value does not matter, so a
        // division by zero there is not an error: if B == 0 then 0 else A div B
        try {
            value(other, symbols);
        } catch (InvalidValueException _) {
            // Ignored, see above
        }
        return value(taken, symbols);
    }

    /**
     * Casts {@code operand} to the integer type {@code target}, truncating a float toward zero.
     *
     * @throws InvalidValueException If {@code operand} is a float that does not fit in {@code target}.
     */
    public static LiteralExpression castToInt(final Node node, final LiteralExpression operand, final Type target) {
        if (operand instanceof IntegerLiteral il) {
            return integer(node, il.asBigInteger(), target);
        }
        final var value = asDouble(operand);
        // LLVM's fptosi truncates toward zero, and gives poison if the result does not fit
        final var truncated = Double.isFinite(value) ? new BigDecimal(value).toBigInteger() : null;
        if (truncated == null || truncated.bitLength() >= bits(target)) {
            final var msg = "value " + value + " does not fit in " + target.getName().toLowerCase() + ": " + node;
            throw new InvalidValueException(msg, String.valueOf(value));
        }
        return integer(node, truncated, target);
    }

    /**
     * Casts {@code operand} to the float type {@code target}.
     */
    public static LiteralExpression castToFloat(final Node node, final LiteralExpression operand, final Type target) {
        if (operand instanceof IntegerLiteral il) {
            // Convert directly to the target precision, rounding only once
            final var integer = il.asBigInteger();
            final double value = (target instanceof F32) ? integer.floatValue() : integer.doubleValue();
            return floating(node, value, target);
        }
        return floating(node, asDouble(operand), target);
    }

    /**
     * Returns a {@link BooleanLiteral} that holds {@code value}.
     */
    public static LiteralExpression booleanLiteral(final Node node, final boolean value) {
        return BooleanLiteral.from(node.line(), node.column(), Boolean.toString(value));
    }

    /**
     * Returns an {@link IntegerLiteral} that holds -1 for true and 0 for false.
     */
    public static LiteralExpression integerBoolean(final Node node, final boolean value) {
        return new IntegerLiteral(node.line(), node.column(), value ? -1L : 0L);
    }

    private static LiteralExpression negate(final NegateExpression ne, final LiteralExpression operand) {
        if (operand instanceof IntegerLiteral il) {
            return integer(ne, il.asBigInteger().negate(), il.type());
        }
        final var fl = asFloat(operand);
        return floating(ne, -asDouble(fl), fl.type());
    }

    private LiteralExpression binary(final BinaryExpression be,
                                     final LiteralExpression left,
                                     final LiteralExpression right) {
        final var evaluator = binaryEvaluators.get(be.getClass());
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
        return arithmetic(be, left, right, BigInteger::add, Double::sum);
    }

    private static LiteralExpression arithmetic(final BinaryExpression be,
                                                final LiteralExpression left,
                                                final LiteralExpression right,
                                                final BinaryOperator<BigInteger> integerOperator,
                                                final DoubleBinaryOperator floatOperator) {
        if (left instanceof IntegerLiteral il) {
            return integer(be, operator(integerOperator).apply(il.asBigInteger(), asBigInteger(right)), il.type());
        }
        final var fl = asFloat(left);
        // Two single precision operands give the correctly rounded single precision result when
        // the operation is done in double precision and then rounded
        return floating(be, operator(floatOperator).applyAsDouble(asDouble(fl), asDouble(right)), fl.type());
    }

    /**
     * Returns {@code operator}, or fails if it is null, because the operation is not defined for
     * the type of its operands, as in 1 and 2.0.
     */
    private static <T> T operator(final T operator) {
        if (operator == null) {
            throw new FailedConstantException();
        }
        return operator;
    }

    private static LiteralExpression division(final BinaryExpression be,
                                              final LiteralExpression left,
                                              final LiteralExpression right,
                                              final BinaryOperator<BigInteger> operator) {
        final var il = asInteger(left);
        final var dividend = il.asBigInteger();
        final var divisor = asBigInteger(right);
        if (divisor.signum() == 0) {
            final var msg = "division by zero: " + be;
            throw new InvalidValueException(msg, right.getValue());
        }
        final var min = BigInteger.ONE.shiftLeft(bits(il.type()) - 1).negate();
        if (dividend.equals(min) && divisor.equals(BigInteger.ONE.negate())) {
            final var msg = "integer overflow: " + be;
            throw new InvalidValueException(msg, il.getValue());
        }
        return integer(be, operator.apply(dividend, divisor), il.type());
    }

    private static boolean isEqual(final LiteralExpression left, final LiteralExpression right) {
        return switch (left) {
            case IntegerLiteral il -> il.asBigInteger().equals(asBigInteger(right));
            case FloatLiteral _ -> asDouble(left) == asDouble(right);
            case BooleanLiteral _ -> asBoolean(left) == asBoolean(right);
            default -> left.getValue().equals(right.getValue());
        };
    }

    /**
     * Compares the operands with primitive operators, which for floats have the semantics of
     * LLVM's ordered predicates: every comparison with NaN is false. Strings are compared like
     * strcmp does at runtime: byte by byte as unsigned values, in UTF-8.
     */
    private LiteralExpression compare(final BinaryExpression be,
                                      final LiteralExpression left,
                                      final LiteralExpression right,
                                      final LongComparison integerComparison,
                                      final DoubleComparison floatComparison) {
        if (left instanceof IntegerLiteral il) {
            return bool(be, integerComparison.test(il.asBigInteger().compareTo(asBigInteger(right)), 0));
        }
        if (left instanceof StringLiteral sl) {
            final var comparison = Arrays.compareUnsigned(sl.getValue().getBytes(UTF_8), asString(right).getBytes(UTF_8));
            return bool(be, integerComparison.test(comparison, 0));
        }
        return bool(be, floatComparison.test(asDouble(left), asDouble(right)));
    }

    private LiteralExpression bool(final Node node, final boolean value) {
        return booleanResult.literal(node, value);
    }

    /**
     * Returns an integer literal of the given type that holds {@code value}, wrapped to the width
     * of the type as a two's complement number.
     */
    private static IntegerLiteral integer(final Node node, final BigInteger value, final Type type) {
        final var bits = bits(type);
        final var modulus = BigInteger.ONE.shiftLeft(bits);
        final var unsigned = value.mod(modulus);
        final var wrapped = unsigned.testBit(bits - 1) ? unsigned.subtract(modulus) : unsigned;
        return new IntegerLiteral(node.line(), node.column(), wrapped.toString(), type);
    }

    /**
     * Returns the width in bits of the given integer type.
     */
    private static int bits(final Type type) {
        return (type instanceof IntegerType it) ? it.bits() : Long.SIZE;
    }

    private static FloatLiteral floating(final Node node, final double value, final Type type) {
        final var text = (type instanceof F32) ? Float.toString((float) value) : Double.toString(value);
        return new FloatLiteral(node.line(), node.column(), text, type);
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

    private static String asString(final LiteralExpression literal) {
        if (literal instanceof StringLiteral sl) {
            return sl.getValue();
        }
        throw new FailedConstantException();
    }

    private static BigInteger asBigInteger(final LiteralExpression literal) {
        return asInteger(literal).asBigInteger();
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
    private interface ArgValue {
        LiteralExpression of(Expression arg);
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
     * Thrown when an expression references a constant whose initializer failed, has an operand of
     * the wrong type, or calls a function that did not resolve. Each has already been reported.
     */
    private static class FailedConstantException extends RuntimeException {
        FailedConstantException() {
            super(null, null, false, false);
        }
    }
}
