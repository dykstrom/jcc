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

import se.dykstrom.jcc.common.ast.CastToF64Expression;
import se.dykstrom.jcc.common.ast.CastToI32Expression;
import se.dykstrom.jcc.common.ast.CastToI64Expression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.ast.RoundExpression;
import se.dykstrom.jcc.common.ast.TruncExpression;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.semantics.expression.FunctionCallSemanticsParser;
import se.dykstrom.jcc.common.types.Identifier;

import static se.dykstrom.jcc.col.compiler.LibJccColBuiltIns.*;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.FUN_ROUND_F64;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.FUN_TRUNC_F64;

public class ColFunctionCallSemanticsParser<T extends TypeManager> extends FunctionCallSemanticsParser<T> {

    public ColFunctionCallSemanticsParser(final SemanticsParser<T> semanticsParser) {
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

        if (symbols().containsFunction(name)) {
            // If the identifier is a function identifier
            try {
                // Match the function with the expected argument types
                Function function = types().resolveFunction(name, actualArgTypes, symbols());
                identifier = function.getIdentifier();
                // Resolve any arguments that need type inference
                args = types().resolveArgs(args, function.getArgTypes());

                // Replace casting and certain function calls with custom expressions
                if (FUN_F64_FROM_I64.getIdentifier().equals(identifier)) {
                    return new CastToF64Expression(expression.line(), expression.column(), args.get(0));
                } else if (FUN_I32_FROM_I64.getIdentifier().equals(identifier)) {
                    return new CastToI32Expression(expression.line(), expression.column(), args.get(0));
                } else if (FUN_I64_FROM_F64.getIdentifier().equals(identifier)) {
                    return new CastToI64Expression(expression.line(), expression.column(), args.get(0));
                } else if (FUN_ROUND_F64.getIdentifier().equals(identifier)) {
                    return new RoundExpression(expression.line(), expression.column(), args.get(0));
                } else if (FUN_TRUNC_F64.getIdentifier().equals(identifier)) {
                    return new TruncExpression(expression.line(), expression.column(), args.get(0));
                }
            } catch (SemanticsException ignore) {
                // Ignore
            }
        }

        return super.parse(expression);
    }
}
