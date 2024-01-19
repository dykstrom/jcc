/*
 * Copyright (C) 2023 Johan Dykstrom
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

import se.dykstrom.jcc.col.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.col.types.ColTypeManager;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.IDivExpression;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.types.I64;

public class IDivSemanticsParser extends AbstractSemanticsParserComponent<ColTypeManager, SemanticsParser<ColTypeManager>>
        implements ExpressionSemanticsParser<IDivExpression> {

    public IDivSemanticsParser(final SemanticsParser<ColTypeManager> semanticsParser) {
        super(semanticsParser);
    }

    @Override
    public Expression parse(final IDivExpression expression) {
        final Expression left = parser.expression(expression.getLeft());
        final Expression right = parser.expression(expression.getRight());
        return checkType(checkDivisionByZero(expression.withLeft(left).withRight(right)));
    }

    @Override
    protected Expression checkType(final Expression expression) {
        final var ie = (IDivExpression) expression;
        final var leftType = getType(ie.getLeft());
        final var rightType = getType(ie.getRight());

        if (isTypeMismatch(I64.class, leftType, rightType)) {
            final var msg = "expected integer subexpressions: " + expression;
            reportError(expression, msg, new SemanticsException(msg));
        }
        return super.checkType(expression);
    }
}
