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

import se.dykstrom.jcc.common.ast.BinaryExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.semantics.expression.RelationalSemanticsParser;
import se.dykstrom.jcc.common.types.Str;

/**
 * Type checks the ordering operators. COL v1 defines no ordering on strings - only equality -
 * so a string operand is rejected here with a message that says so, rather than falling through
 * to the shared numeric check and its generic "cannot compare" message.
 * <p>
 * This rule is COL's, not a general one: other languages targeting the shared relational parser
 * may well order strings.
 */
public class ColRelationalSemanticsParser<T extends TypeManager> extends RelationalSemanticsParser<T> {

    public ColRelationalSemanticsParser(final SemanticsParser<T> semanticsParser) {
        super(semanticsParser);
    }

    @Override
    protected Expression checkType(final Expression expression) {
        final var e = (BinaryExpression) expression;
        if (getType(e.getLeft()) instanceof Str || getType(e.getRight()) instanceof Str) {
            final var msg = "cannot order strings: only == and != are defined for string";
            reportError(expression, msg, new SemanticsException(msg));
            return expression;
        }
        return super.checkType(expression);
    }
}
