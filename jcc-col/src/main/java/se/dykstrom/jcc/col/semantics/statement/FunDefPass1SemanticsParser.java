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

package se.dykstrom.jcc.col.semantics.statement;

import se.dykstrom.jcc.common.ast.Declaration;
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.error.SemanticsException;
import se.dykstrom.jcc.common.functions.UserDefinedFunction;
import se.dykstrom.jcc.common.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.common.semantics.statement.StatementSemanticsParser;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.types.Void;

import java.util.ArrayList;
import java.util.List;

public class FunDefPass1SemanticsParser<T extends TypeManager> extends AbstractSemanticsParserComponent<T>
        implements StatementSemanticsParser<FunctionDefinitionStatement> {

    public FunDefPass1SemanticsParser(final SemanticsParser<T> semanticsParser) {
        super(semanticsParser);
    }

    @Override
    public Statement parse(final FunctionDefinitionStatement statement) {
        final var identifier = statement.identifier();
        final var functionType = (Fun) identifier.type();
        final var declarations = statement.declarations();

        final var argNames = declarations.stream().map(Declaration::name).toList();
        final var argTypes = declarations.stream()
                                         .map(declaration -> resolveType(statement, declaration.type(), types()))
                                         .toList();
        final var returnType = resolveType(statement, functionType.getReturnType(), types());

        // An omitted parameter type or return type is resolved to void; reject it with a message
        // that names the rule, and skip defining a function with an incomplete signature.
        if (!checkSignature(statement, argNames, argTypes, returnType)) {
            return statement;
        }

        final var function = new UserDefinedFunction(identifier.name(), argNames, argTypes, returnType);

        // Define function in symbol table
        defineFunction(statement, function);

        final var updatedFunctionType = Fun.from(argTypes, returnType);
        final var updatedDeclarations = updateDeclarations(declarations, argTypes);
        return statement.withIdentifier(identifier.withType(updatedFunctionType))
                        .withDeclarations(updatedDeclarations);
    }

    private boolean checkSignature(final FunctionDefinitionStatement statement,
                                   final List<String> argNames,
                                   final List<Type> argTypes,
                                   final Type returnType) {
        boolean complete = true;
        for (int i = 0; i < argNames.size(); i++) {
            if (argTypes.get(i) instanceof Void) {
                final var msg = "parameter '" + argNames.get(i) + "' must declare a type";
                reportError(statement, msg, new SemanticsException(msg));
                complete = false;
            }
        }
        if (returnType instanceof Void) {
            final var msg = "function '" + statement.identifier().name() + "' must declare a return type";
            reportError(statement, msg, new SemanticsException(msg));
            complete = false;
        }
        return complete;
    }

    private List<Declaration> updateDeclarations(final List<Declaration> declarations, final List<Type> argTypes) {
        final var result = new ArrayList<Declaration>();
        for (int i = 0; i < declarations.size(); i++) {
            result.add(declarations.get(i).withType(argTypes.get(i)));
        }
        return result;
    }
}
