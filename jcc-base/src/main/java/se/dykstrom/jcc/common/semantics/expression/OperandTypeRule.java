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

package se.dykstrom.jcc.common.semantics.expression;

import se.dykstrom.jcc.common.ast.BinaryExpression;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

import java.util.function.BiPredicate;

import static java.util.Objects.requireNonNull;

/**
 * What a binary operator demands of its operand types, and what to say when the operands do not
 * meet it. A rule is composed into a {@link BinarySemanticsParser} rather than subclassed onto one,
 * so an operator is defined by the rules it is given: {@code NUMERIC} for arithmetic,
 * {@code INTEGER} for the bitwise operators, {@code NUMERIC.or(STRINGS)} for an addition that also
 * concatenates, and no rule at all for equality, which accepts any two operands of the same type.
 * <p>
 * A language with its own demands defines its own rule with {@link #of} - no subclass needed.
 */
public final class OperandTypeRule {

    /** Both operands must be numbers. Arithmetic and the ordering operators. */
    public static final OperandTypeRule NUMERIC =
            of((left, right) -> left.isNumber() && right.isNumber(), OperandTypeRule::cannotOperate);

    /** Both operands must be integers. The bitwise operators, {@code div} and {@code mod}. */
    public static final OperandTypeRule INTEGER =
            of((left, right) -> left.isInteger() && right.isInteger(), expected("integer"));

    /** Both operands must be floating point. Floating point division. */
    public static final OperandTypeRule FLOAT =
            of((left, right) -> left.isFloat() && right.isFloat(), expected("floating point"));

    /** Both operands must be booleans. The logical operators. */
    public static final OperandTypeRule BOOLEAN =
            of((left, right) -> left instanceof Bool && right instanceof Bool, expected("boolean"));

    /** Both operands must be strings. Only useful combined with another rule, e.g. {@code NUMERIC.or(STRINGS)}. */
    public static final OperandTypeRule STRINGS =
            of((left, right) -> left instanceof Str && right instanceof Str, OperandTypeRule::cannotOperate);

    private final BiPredicate<Type, Type> predicate;
    private final Message message;

    private OperandTypeRule(final BiPredicate<Type, Type> predicate, final Message message) {
        this.predicate = requireNonNull(predicate);
        this.message = requireNonNull(message);
    }

    /**
     * Creates a rule that accepts the operand types matching the given predicate, and reports the
     * message the given factory produces for those it rejects.
     */
    public static OperandTypeRule of(final BiPredicate<Type, Type> predicate, final Message message) {
        return new OperandTypeRule(predicate, message);
    }

    /**
     * Returns a rule accepting what either this rule or the given one accepts. The combined rule
     * reports <em>this</em> rule's message, since it names the operator's primary intent: an
     * addition that also concatenates still reports "cannot add string and i64".
     */
    public OperandTypeRule or(final OperandTypeRule other) {
        return new OperandTypeRule(predicate.or(other.predicate), message);
    }

    /** Returns whether this rule accepts the given operand types. */
    public boolean accepts(final Type left, final Type right) {
        return predicate.test(left, right);
    }

    /** Returns the error message to report for operands this rule rejects. */
    public String message(final Operands operands) {
        return message.create(operands);
    }

    /**
     * The rejected expression and everything a message needs to describe it. The operand types are
     * passed in rather than looked up again, so that a message sees the same types the check did -
     * including the {@code I64} an untyped operand degrades to (see {@code col-error-reporting.md}).
     */
    public record Operands(BinaryExpression expression, Type left, Type right, TypeManager types, String operation) { }

    /** Produces the error message for operands a rule rejects. */
    @FunctionalInterface
    public interface Message {
        String create(Operands operands);
    }

    private static String cannotOperate(final Operands operands) {
        return "cannot " + operands.operation() + " " +
               operands.types().getTypeName(operands.left()) + " and " +
               operands.types().getTypeName(operands.right());
    }

    private static Message expected(final String typeDescription) {
        return operands -> "expected " + typeDescription + " subexpressions: " + operands.expression();
    }
}
