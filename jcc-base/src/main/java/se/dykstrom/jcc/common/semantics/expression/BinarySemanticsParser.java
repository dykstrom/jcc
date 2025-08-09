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

import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;

import static se.dykstrom.jcc.common.compiler.AbstractTypeManager.canBePromoted;
import static se.dykstrom.jcc.common.compiler.TypeManager.isFloat;
import static se.dykstrom.jcc.common.compiler.TypeManager.isInteger;

public abstract class BinarySemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements ExpressionSemanticsParser<BinaryExpression> {

    protected final String operation;

    public BinarySemanticsParser(final SemanticsParser<T> semanticsParser, final String operation) {
        super(semanticsParser);
        this.operation = operation;
    }

    @Override
    public Expression parse(final BinaryExpression expression) {
        final var left = parser.expression(expression.getLeft());
        final var right = parser.expression(expression.getRight());
        return checkType(expression.withLeft(left).withRight(right));
    }

    @Override
    protected Expression checkType(final Expression expression) {
        final var e = (BinaryExpression) expression;
        final var left = e.getLeft();
        final var right = e.getRight();
        final var lt = getType(left);
        final var rt = getType(right);

        // If the types are not the same, check if one can be promoted to the other, and insert a cast expression
        // At the moment, we can only promote i32 to i64 and f32 to f64
        if (lt.equals(rt)) {
            return super.checkType(expression);
        } else if (isInteger(rt) && canBePromoted(lt, rt)) {
            return super.checkType(e.withLeft(new CastToI64Expression(left.line(), left.column(), left)));
        } else if (isInteger(lt) && canBePromoted(rt, lt)) {
            return super.checkType(e.withRight(new CastToI64Expression(right.line(), right.column(), right)));
        } else if (isFloat(rt) && canBePromoted(lt, rt)) {
            return super.checkType(e.withLeft(new CastToF64Expression(left.line(), left.column(), left)));
        } else if (isFloat(lt) && canBePromoted(rt, lt)) {
            return super.checkType(e.withRight(new CastToF64Expression(right.line(), right.column(), right)));
        } else {
            final var msg = "cannot " + operation + " " + types().getTypeName(lt) + " and " + types().getTypeName(rt);
            reportError(expression, msg, new SemanticsException(msg));
            return expression;
        }
    }
}
