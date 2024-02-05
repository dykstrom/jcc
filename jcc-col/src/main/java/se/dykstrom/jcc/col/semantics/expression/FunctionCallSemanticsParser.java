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
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.error.UndefinedException;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Identifier;

public class FunctionCallSemanticsParser extends AbstractSemanticsParserComponent<ColTypeManager, SemanticsParser<ColTypeManager>>
        implements ExpressionSemanticsParser<FunctionCallExpression> {

    public FunctionCallSemanticsParser(final SemanticsParser<ColTypeManager> semanticsParser) {
        super(semanticsParser);
    }

    @Override
    public Expression parse(final FunctionCallExpression expression) {
        // Check and update arguments
        var args = expression.getArgs().stream().map(parser::expression).toList();
        // Get types of arguments
        final var argTypes = types().getTypes(args);

        Identifier identifier = expression.getIdentifier();
        String name = identifier.name();

        if (symbols().containsFunction(name)) {
            // If the identifier is a function identifier
            try {
                // Match the function with the expected argument types
                Function function = types().resolveFunction(name, argTypes, symbols());
                identifier = function.getIdentifier();
                // Resolve any arguments that need type inference
                args = types().resolveArgs(args, function.getArgTypes());
            } catch (SemanticsException e) {
                reportError(expression, e.getMessage(), e);
                // Make sure the type is a function, so we can continue parsing
                identifier = identifier.withType(Fun.from(argTypes, I64.INSTANCE));
            }
        } else {
            String msg = "undefined function: " + name;
            reportError(expression, msg, new UndefinedException(msg, name));
        }

        return expression.withIdentifier(identifier).withArgs(args);
    }
}
