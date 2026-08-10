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

/**
 * What a binary operator demands of its operand <em>values</em>, as opposed to their types. Only
 * division needs anything here, so this is deliberately a closed set rather than an extension
 * point; composed into a {@link BinarySemanticsParser} alongside its {@link OperandTypeRule}s.
 */
public enum OperandValueRule {

    /** No demand on the operand values. */
    ANY,

    /** A constant zero divisor is a compile-time error. Floating point and integer division, and mod. */
    NON_ZERO_DIVISOR
}
