/*
 * Copyright (C) 2024 Johan Dykstrom
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
import se.dykstrom.jcc.common.ast.LogicalNotExpression;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.InvalidTypeException;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.types.Bool;

public class LogicalNotSemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements ExpressionSemanticsParser<LogicalNotExpression> {

    public LogicalNotSemanticsParser(final SemanticsParser<T> semanticsParser) {
        super(semanticsParser);
    }

    @Override
    public Expression parse(final LogicalNotExpression expression) {
        final var subExpression = parser.expression(expression.getExpression());
        final var type = getType(subExpression);

        // Logical expressions require the subexpressions to be booleans
        if (!(type instanceof Bool)) {
            String msg = "expected boolean subexpression: " + expression;
            reportError(expression, msg, new InvalidTypeException(msg, type));
        }

        return expression.withExpression(subExpression);
    }
}
