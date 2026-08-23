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
import se.dykstrom.jcc.common.ast.LiteralExpression;
import se.dykstrom.jcc.common.error.InvalidValueException;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.utils.ExpressionUtils;

import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * What an operator demands of its operand <em>values</em>, as opposed to their types. Composed into
 * a {@link BinarySemanticsParser} alongside its {@link OperandTypeRule}s, and shaped like one: a
 * rule {@link #accepts} an expression, or supplies the exception to report for one it rejects.
 * Division is the only operator that needs anything here so far.
 */
public final class OperandValueRule {

    /** No demand on the operand values, so nothing can violate it and no exception exists. */
    public static final OperandValueRule ANY = new OperandValueRule(
            expression -> true,
            expression -> { throw new IllegalStateException("ANY accepts every expression"); });

    /** A constant zero divisor is a compile-time error. Floating point and integer division, and mod. */
    public static final OperandValueRule NON_ZERO_DIVISOR = new OperandValueRule(
            expression -> !ExpressionUtils.isZeroDivisor(expression),
            expression -> {
                final var value = ((LiteralExpression) expression.getRight()).getValue();
                return new InvalidValueException("division by zero: " + value, value);
            });

    private final Predicate<BinaryExpression> predicate;
    private final Function<BinaryExpression, SemanticsException> exception;

    private OperandValueRule(final Predicate<BinaryExpression> predicate,
                             final Function<BinaryExpression, SemanticsException> exception) {
        this.predicate = requireNonNull(predicate);
        this.exception = requireNonNull(exception);
    }

    /** Returns whether this rule accepts the given expression's operand values. */
    public boolean accepts(final BinaryExpression expression) {
        return predicate.test(expression);
    }

    /** Returns the exception to report for an expression this rule rejects. */
    public SemanticsException exception(final BinaryExpression expression) {
        return exception.apply(expression);
    }
}
