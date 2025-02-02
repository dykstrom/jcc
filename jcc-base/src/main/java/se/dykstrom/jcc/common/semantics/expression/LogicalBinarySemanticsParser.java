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

import se.dykstrom.jcc.common.ast.BinaryExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.types.Bool;

public class LogicalBinarySemanticsParser<T extends TypeManager> extends BinarySemanticsParser<T> {

    public LogicalBinarySemanticsParser(final SemanticsParser<T> semanticsParser, final String operation) {
        super(semanticsParser, operation);
    }

    @Override
    protected Expression checkType(final Expression expression) {
        final var e = (BinaryExpression) expression;
        final var leftType = getType(e.getLeft());
        final var rightType = getType(e.getRight());

        // Logical expressions require the subexpressions to be booleans
        if (!(leftType instanceof Bool) || !(rightType instanceof Bool)) {
            String msg = "expected boolean subexpressions: " + expression;
            reportError(expression, msg, new SemanticsException(msg));
        }

        return super.checkType(expression);
    }
}
