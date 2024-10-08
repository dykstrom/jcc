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

package se.dykstrom.jcc.common.code.statement;

import se.dykstrom.jcc.common.ast.Declaration;
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.functions.UserDefinedFunction;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.types.Fun;

import java.util.List;

public class FunctionDefinitionCodeGenerator extends AbstractStatementCodeGenerator<FunctionDefinitionStatement, TypeManager, AbstractCodeGenerator> {

    public FunctionDefinitionCodeGenerator(final AbstractCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(final FunctionDefinitionStatement statement) {
        if (statement.expression() == null) {
            throw new UnsupportedOperationException("only expression functions supported");
        }
        // Create function
        final var argNames = statement.declarations().stream().map(Declaration::name).toList();
        final var argTypes = statement.declarations().stream().map(Declaration::type).toList();
        final var returnType = ((Fun) statement.identifier().type()).getReturnType();

        final var functionName = statement.identifier().name();
        final var function = new UserDefinedFunction(functionName, argNames, argTypes, returnType);
        if (!symbols().containsFunction(functionName, argTypes)) {
            symbols().addFunction(function);
        }
        codeGenerator.addUserDefinedFunction(function, statement.expression());
        return List.of();
    }
}
