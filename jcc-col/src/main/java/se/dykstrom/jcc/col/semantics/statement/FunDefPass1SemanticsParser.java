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

import java.util.ArrayList;
import java.util.List;

import se.dykstrom.jcc.col.semantics.AbstractSemanticsParserComponent;
import se.dykstrom.jcc.col.types.ColTypeManager;
import se.dykstrom.jcc.common.ast.Declaration;
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.compiler.SemanticsParser;
import se.dykstrom.jcc.common.functions.UserDefinedFunction;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.Type;

public class FunDefPass1SemanticsParser extends AbstractSemanticsParserComponent<ColTypeManager, SemanticsParser<ColTypeManager>>
        implements StatementSemanticsParser<FunctionDefinitionStatement> {

    public FunDefPass1SemanticsParser(final SemanticsParser<ColTypeManager> semanticsParser) {
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
        final var function = new UserDefinedFunction(identifier.name(), argNames, argTypes, returnType);

        // Define function in symbol table
        defineFunction(statement, function);

        final var updatedFunctionType = Fun.from(argTypes, returnType);
        final var updatedDeclarations = updateDeclarations(declarations, argTypes);
        return statement.withIdentifier(identifier.withType(updatedFunctionType))
                        .withDeclarations(updatedDeclarations);
    }

    private List<Declaration> updateDeclarations(final List<Declaration> declarations, final List<Type> argTypes) {
        final var result = new ArrayList<Declaration>();
        for (int i = 0; i < declarations.size(); i++) {
            result.add(declarations.get(i).withType(argTypes.get(i)));
        }
        return result;
    }
}
