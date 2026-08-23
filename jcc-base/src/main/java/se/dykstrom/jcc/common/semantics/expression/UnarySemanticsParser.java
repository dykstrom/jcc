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

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.UnaryExpression;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.InvalidTypeException;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.types.Type;

import static java.util.Objects.requireNonNull;

/**
 * Type checks a unary expression: what the operator demands of its operand. The unary counterpart of
 * {@link BinarySemanticsParser}, composed the same way and from the same rules - an operator is
 * defined by the {@code operation} verb used in its error message ("cannot <em>negate</em> bool") and
 * the {@link OperandTypeRule}s its operand must satisfy, not by a subclass per operator.
 * <pre>
 * new UnarySemanticsParser&lt;&gt;(this, "negate", NUMERIC)
 * new UnarySemanticsParser&lt;&gt;(this, "bitwise-not", INTEGER)
 * </pre>
 *
 * @author Johan Dykstrom
 */
public class UnarySemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements ExpressionSemanticsParser<UnaryExpression> {

    private final String operation;
    private final OperandTypeRule typeRule;

    public UnarySemanticsParser(final SemanticsParser<T> semanticsParser,
                                final String operation,
                                final OperandTypeRule typeRule) {
        super(semanticsParser);
        this.operation = requireNonNull(operation);
        this.typeRule = requireNonNull(typeRule);
    }

    @Override
    public Expression parse(final UnaryExpression expression) {
        final var operand = parser.expression(expression.getExpression());
        checkOperandType(expression, getType(operand));
        return expression.withExpression(operand);
    }

    /** Reports the operand type if the rule rejects it. */
    private void checkOperandType(final UnaryExpression expression, final Type type) {
        if (!typeRule.accepts(type)) {
            final var msg = typeRule.message(OperandTypeRule.Operands.of(types(), operation, type));
            reportError(expression, msg, new InvalidTypeException(msg, type));
        }
    }
}
