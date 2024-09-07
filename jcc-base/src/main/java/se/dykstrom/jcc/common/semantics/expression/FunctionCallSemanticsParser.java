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

package se.dykstrom.jcc.common.semantics.expression;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.error.UndefinedException;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;

public class FunctionCallSemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements ExpressionSemanticsParser<FunctionCallExpression> {

    public FunctionCallSemanticsParser(final SemanticsParser<T> semanticsParser) {
        super(semanticsParser);
    }

    @Override
    public Expression parse(final FunctionCallExpression expression) {
        // Check and update arguments
        var args = expression.getArgs().stream().map(parser::expression).toList();
        // Get types of arguments
        final var actualArgTypes = types().getTypes(args);

        Identifier identifier = expression.getIdentifier();
        String name = identifier.name();

        if (symbols().containsFunction(name) || symbols().contains(name)) {
            // If the identifier is a function identifier
            try {
                // Match the function with the expected argument types
                Function function = types().resolveFunction(name, actualArgTypes, symbols());
                identifier = function.getIdentifier();
                // Resolve any arguments that need type inference
                args = types().resolveArgs(args, function.getArgTypes());
            } catch (SemanticsException e) {
                reportError(expression, e.getMessage(), e);
                // Make sure the type is a function, so we can continue parsing
                identifier = identifier.withType(Fun.from(actualArgTypes, I64.INSTANCE));
            }
        } else {
            String msg = "undefined function: " + name;
            reportError(expression, msg, new UndefinedException(msg, name));
        }

        return expression.withIdentifier(identifier).withArgs(args);
    }
}
