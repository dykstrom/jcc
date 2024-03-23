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
import se.dykstrom.jcc.common.types.AmbiguousType;

import static java.util.stream.Collectors.toSet;

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
        } else if (symbols().containsFunction(name)) {
            final var functions = symbols().getFunctions(name);
            if (functions.size() == 1) {
                // If there is only one function with this name, we have found a match
                final var functionIdentifier = functions.iterator().next().getIdentifier();
                return expression.withIdentifier(functionIdentifier);
            } else {
                // If there are several overloaded functions, we don't know which one to use
                // We need to use type inference where the expression is used
                final var functionTypes = functions.stream()
                                                   .map(f -> f.getIdentifier().type())
                                                   .collect(toSet());
                final var oneOfManyType = new AmbiguousType(functionTypes);
                final var functionIdentifier = expression.getIdentifier().withType(oneOfManyType);
                return expression.withIdentifier(functionIdentifier);
            }
        } else {
            final var msg = "undefined variable: " + name;
            reportError(expression, msg, new UndefinedException(msg, name));
            return expression;
        }
    }
}
