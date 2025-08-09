/*
 * Copyright (C) 2025 Johan Dykstrom
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
import se.dykstrom.jcc.common.ast.IfExpression;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.InvalidTypeException;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.types.Bool;

import static se.dykstrom.jcc.common.compiler.AbstractTypeManager.canBePromoted;
import static se.dykstrom.jcc.common.compiler.AbstractTypeManager.promoteTo;

public class IfSemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements ExpressionSemanticsParser<IfExpression> {

    public IfSemanticsParser(final SemanticsParser<T> semanticsParser) {
        super(semanticsParser);
    }

    @Override
    public Expression parse(final IfExpression expression) {
        final var ifExpr = parser.expression(expression.ifExpr());
        final var thenExpr = parser.expression(expression.thenExpr());
        final var elseExpr = parser.expression(expression.elseExpr());
        final var it = getType(ifExpr);
        final var tt = getType(thenExpr);
        final var et = getType(elseExpr);

        if (!(it instanceof Bool)) {
            final var msg = "expected boolean expression, found: " + expression.ifExpr();
            reportError(expression.ifExpr(), msg, new InvalidTypeException(msg, it));
        }

        if (tt.equals(et)) {
            return expression.withIfExpr(ifExpr).withThenExpr(thenExpr).withElseExpr(elseExpr);
        }
        if (canBePromoted(tt, et)) {
            return expression.withIfExpr(ifExpr).withThenExpr(promoteTo(thenExpr, et)).withElseExpr(elseExpr);
        }
        if (canBePromoted(et, tt)) {
            return expression.withIfExpr(ifExpr).withThenExpr(thenExpr).withElseExpr(promoteTo(elseExpr, tt));
        }

        final var msg = "both branches of an if expression must have the same type, found: " +
                types().getTypeName(tt) + " and " + types().getTypeName(et);
        reportError(expression, msg, new SemanticsException(msg));
        return expression;
    }
}
