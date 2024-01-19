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

package se.dykstrom.jcc.col.semantics.expression;

import se.dykstrom.jcc.col.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.col.types.ColTypeManager;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.error.UndefinedException;

public class IdentifierDerefSemanticsParser extends AbstractSemanticsParserComponent<ColTypeManager, SemanticsParser<ColTypeManager>>
        implements ExpressionSemanticsParser<IdentifierDerefExpression> {

    public IdentifierDerefSemanticsParser(final SemanticsParser<ColTypeManager> semanticsParser) {
        super(semanticsParser);
    }

    @Override
    public Expression parse(final IdentifierDerefExpression expression) {
        final var name = expression.getIdentifier().name();
        if (symbols().contains(name)) {
            // Use the identifier from the symbol table
            final var identifier = symbols().getIdentifier(name);
            return expression.withIdentifier(identifier);
        } else {
            final var msg = "undefined variable: " + name;
            reportSemanticsError(expression, msg, new UndefinedException(msg, name));
            return expression;
        }
    }
}
