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

package se.dykstrom.jcc.col.semantics.expression;

import se.dykstrom.jcc.common.semantics.expression.OperandTypeRule;
import se.dykstrom.jcc.common.semantics.expression.OperandTypeRule.Operands;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.Opaque;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

import java.util.List;
import java.util.function.Predicate;

import static se.dykstrom.jcc.common.semantics.expression.OperandTypeRule.cannotOperate;

/**
 * Operand rules that are COL's own, rather than general to every language jcc compiles.
 */
public final class ColOperandTypeRules {

    private static final String NO_FUNCTION_OPERATORS = ": an opaque function type supports no operators";

    /**
     * Rejects a string operand. Composed into the ordering operators ahead of
     * {@link OperandTypeRule#NUMERIC}, so that {@code "a" < "b"} says ordering is not defined for
     * strings instead of the generic "cannot compare". COL v1 defines only equality on strings;
     * this is a COL decision, not a general one - other languages order strings perfectly well.
     */
    public static final OperandTypeRule NOT_STRINGS = OperandTypeRule.ofEachOperand(
            type -> !(type instanceof Str),
            operands -> "cannot order strings: only == and != are defined for strings");

    /**
     * Accepts operands that are all the same opaque type. Combined with {@code or} into the operand
     * rule of an operator an opaque type inherits, behind one of the {@code OPAQUE_*} rules, which
     * report what is wrong with opaque operands.
     */
    public static final OperandTypeRule SAME_OPAQUE =
            OperandTypeRule.of(ColOperandTypeRules::isSameOpaque, OperandTypeRule::cannotOperate);

    /** The opaque rule of {@code ==} and {@code !=}, which an opaque type inherits from every underlying type but a function type. */
    public static final OperandTypeRule OPAQUE_EQUALITY = inheritedByOpaque(type -> !(type instanceof Fun));

    /** The opaque rule of {@code +}, which adds numbers and concatenates strings. */
    public static final OperandTypeRule OPAQUE_ADD = inheritedByOpaque(type -> type.isNumber() || type instanceof Str);

    /** The opaque rule of {@code -} and the ordering operators, which are defined only for numbers. */
    public static final OperandTypeRule OPAQUE_NUMERIC = inheritedByOpaque(Type::isNumber);

    /** Rejects any opaque operand. Composed first into every operator an opaque type does not inherit. */
    public static final OperandTypeRule NOT_OPAQUE = OperandTypeRule.ofEachOperand(
            type -> !(type instanceof Opaque),
            operands -> isOpaqueFunction(operands.operandTypes())
                        ? cannotOperate(operands) + NO_FUNCTION_OPERATORS
                        : cannotOperate(operands) + ": an opaque type inherits only ==, !=, ordering, + and -");

    private ColOperandTypeRules() { }

    /**
     * Creates the rule of an operator that an opaque type inherits if its underlying type matches
     * {@code underlying}. The rule accepts operands with no opaque type among them, and leaves those
     * to the operator's other rules. It is composed first, so that an opaque mistake is reported
     * here and not as the generic "cannot add Meters and f64".
     */
    private static OperandTypeRule inheritedByOpaque(final Predicate<Type> underlying) {
        return OperandTypeRule.of(
                types -> types.stream().noneMatch(Opaque.class::isInstance) ||
                         (isSameOpaque(types) && underlying.test(Opaque.unwrap(types.getFirst()))),
                ColOperandTypeRules::opaqueMessage);
    }

    private static boolean isSameOpaque(final List<Type> types) {
        return types.getFirst() instanceof Opaque && types.stream().allMatch(types.getFirst()::equals);
    }

    private static boolean isOpaqueFunction(final List<Type> types) {
        return types.stream().anyMatch(type -> type instanceof Opaque opaque && opaque.underlying() instanceof Fun);
    }

    private static String opaqueMessage(final Operands operands) {
        final var types = operands.operandTypes();
        final var typeManager = operands.typeManager();
        if (isOpaqueFunction(types)) {
            return cannotOperate(operands) + NO_FUNCTION_OPERATORS;
        } else if (isSameOpaque(types)) {
            final var underlying = typeManager.getTypeName(Opaque.unwrap(types.getFirst()));
            return cannotOperate(operands) + ": its underlying type " + underlying + " does not support this operator";
        } else if (types.stream().allMatch(Opaque.class::isInstance)) {
            return cannotOperate(operands) + ": both operands must be the same opaque type";
        } else {
            final var opaque = (Opaque) types.stream().filter(Opaque.class::isInstance).findFirst().orElseThrow();
            final var name = typeManager.getTypeName(opaque);
            final var underlying = typeManager.getTypeName(opaque.underlying());
            return cannotOperate(operands) + ": both operands must be " + name +
                   ", wrap with " + name + "(...) or unwrap with " + underlying + "(...)";
        }
    }
}
