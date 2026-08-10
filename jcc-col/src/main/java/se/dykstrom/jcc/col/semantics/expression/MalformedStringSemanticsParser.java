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

import se.dykstrom.jcc.col.ast.expression.MalformedStringLiteral;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.semantics.expression.ExpressionSemanticsParser;

/**
 * Rejects a string literal that could not be decoded, reporting the reason recorded when the
 * literal was parsed - an unknown escape, a malformed or out-of-range {@code &#92;u{...}}, or an
 * embedded NUL.
 */
public class MalformedStringSemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements ExpressionSemanticsParser<MalformedStringLiteral> {

    public MalformedStringSemanticsParser(final SemanticsParser<T> semanticsParser) {
        super(semanticsParser);
    }

    @Override
    public Expression parse(final MalformedStringLiteral expression) {
        final var msg = expression.reason();
        reportError(expression, msg, new SemanticsException(msg));
        return expression;
    }
}
