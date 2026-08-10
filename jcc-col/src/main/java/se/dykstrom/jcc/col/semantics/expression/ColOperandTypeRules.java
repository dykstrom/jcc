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
import se.dykstrom.jcc.common.types.Str;

/**
 * Operand rules that are COL's own, rather than general to every language jcc compiles.
 */
public final class ColOperandTypeRules {

    /**
     * Rejects a string operand. Composed into the ordering operators ahead of
     * {@link OperandTypeRule#NUMERIC}, so that {@code "a" < "b"} says ordering is not defined for
     * strings instead of the generic "cannot compare". COL v1 defines only equality on strings;
     * this is a COL decision, not a general one - other languages order strings perfectly well.
     */
    public static final OperandTypeRule NOT_STRINGS = OperandTypeRule.of(
            (left, right) -> !(left instanceof Str) && !(right instanceof Str),
            operands -> "cannot order strings: only == and != are defined for string");

    private ColOperandTypeRules() { }
}
