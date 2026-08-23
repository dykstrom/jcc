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

import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

import java.util.List;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * What an operator demands of its operand types, and what to say when the operands do not meet it.
 * A rule is composed into a {@link BinarySemanticsParser} or a {@link UnarySemanticsParser} rather
 * than subclassed onto one, so an operator is defined by the rules it is given: {@code NUMERIC} for
 * arithmetic and for negation, {@code INTEGER} for the bitwise operators, {@code NUMERIC.or(STRINGS)}
 * for an addition that also concatenates, and no rule at all for equality, which accepts any two
 * operands of the same type.
 * <p>
 * A rule is arity independent, which is what keeps a unary operator's diagnostics in step with a
 * binary one's: {@code NUMERIC} means "every operand is a number" whether there is one operand or
 * two, and the message it produces names the operand types either way - "cannot negate string",
 * "cannot add string and i64". Only the wording of a narrower rule's requirement clause varies with
 * the number of operands.
 * <p>
 * A language with its own demands defines its own rule with {@link #ofEachOperand} for a demand on
 * each operand separately, or {@link #of} for one that relates the operands to each other.
 */
public final class OperandTypeRule {

    /** Every operand must be a number. Arithmetic, the ordering operators, negation. */
    public static final OperandTypeRule NUMERIC =
            ofEachOperand(Type::isNumber, OperandTypeRule::cannotOperate);

    /** Every operand must be an integer. The bitwise operators, {@code div} and {@code mod}. */
    public static final OperandTypeRule INTEGER =
            ofEachOperand(Type::isInteger, requires("an integer", "integers"));

    /** Every operand must be floating point. Floating point division. */
    public static final OperandTypeRule FLOAT =
            ofEachOperand(Type::isFloat, requires("floating point"));

    /** Every operand must be a boolean. The logical operators. */
    public static final OperandTypeRule BOOLEAN =
            ofEachOperand(type -> type instanceof Bool, requires("boolean"));

    /** Every operand must be a string. Only useful combined with another rule, e.g. {@code NUMERIC.or(STRINGS)}. */
    public static final OperandTypeRule STRINGS =
            ofEachOperand(type -> type instanceof Str, OperandTypeRule::cannotOperate);

    private final Predicate<List<Type>> predicate;
    private final Message message;

    private OperandTypeRule(final Predicate<List<Type>> predicate, final Message message) {
        this.predicate = requireNonNull(predicate);
        this.message = requireNonNull(message);
    }

    /**
     * Creates a rule that accepts the operand types matching the given predicate, and reports the
     * message the given factory produces for those it rejects. For a demand on each operand
     * separately, prefer {@link #ofEachOperand}.
     */
    public static OperandTypeRule of(final Predicate<List<Type>> predicate, final Message message) {
        return new OperandTypeRule(predicate, message);
    }

    /**
     * Creates a rule that accepts operands whose types all match the given predicate. Every rule
     * defined here is of this shape, and so is a rule that rejects a type outright - "no operand is
     * a string" is "every operand is not a string".
     */
    public static OperandTypeRule ofEachOperand(final Predicate<Type> predicate, final Message message) {
        return new OperandTypeRule(types -> types.stream().allMatch(predicate), message);
    }

    /**
     * Returns a rule accepting what either this rule or the given one accepts. The combined rule
     * reports <em>this</em> rule's message, since it names the operator's primary intent: an
     * addition that also concatenates still reports "cannot add string and i64".
     */
    public OperandTypeRule or(final OperandTypeRule other) {
        return new OperandTypeRule(predicate.or(other.predicate), message);
    }

    /** Returns whether this rule accepts the given operand types, in operand order. */
    public boolean accepts(final Type... types) {
        return predicate.test(List.of(types));
    }

    /** Returns the error message to report for operands this rule rejects. */
    public String message(final Operands operands) {
        return message.create(operands);
    }

    /**
     * The operand types a rule rejected, and what a message needs to describe them. The types are
     * passed in rather than looked up again, so that a message sees the same types the check did -
     * including the {@code I64} an untyped operand degrades to (see {@code col-error-reporting.md}).
     * The rejected expression is deliberately absent: a message that rendered it would leak the
     * AST's own spelling, printing {@code mod} as {@code %} and {@code true} as {@code -1}.
     */
    public record Operands(List<Type> operandTypes, TypeManager typeManager, String operation) {

        public Operands(final List<Type> operandTypes, final TypeManager typeManager, final String operation) {
            this.operandTypes = List.copyOf(operandTypes);
            this.typeManager = requireNonNull(typeManager);
            this.operation = requireNonNull(operation);
        }

        /** Creates the operands of an expression whose operand types are, in order, {@code types}. */
        public static Operands of(final TypeManager typeManager, final String operation, final Type... types) {
            return new Operands(List.of(types), typeManager, operation);
        }
    }

    /** Produces the error message for operands a rule rejects. */
    @FunctionalInterface
    public interface Message {
        String create(Operands operands);
    }

    private static String cannotOperate(final Operands operands) {
        return "cannot " + operands.operation() + " " +
               operands.operandTypes().stream()
                       .map(operands.typeManager()::getTypeName)
                       .collect(joining(" and "));
    }

    /** A requirement whose wording does not depend on the number of operands. */
    private static Message requires(final String description) {
        return requires(description, description);
    }

    /**
     * Produces {@link #cannotOperate}'s sentence followed by what the operator demands, for a rule
     * narrower than "every operand is a number". Naming the types is what makes the message
     * actionable, and the clause is what the types alone cannot say - that {@code /} wants floating
     * point operands rather than that {@code i64 / i64} is impossible.
     */
    private static Message requires(final String oneOperand, final String severalOperands) {
        return operands -> cannotOperate(operands) + ((operands.operandTypes().size() == 1)
                ? ": the operand must be " + oneOperand
                : ": both operands must be " + severalOperands);
    }
}
