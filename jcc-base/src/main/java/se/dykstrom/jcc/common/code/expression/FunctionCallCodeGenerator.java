/*
 * Copyright (C) 2021 Johan Dykstrom
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

package se.dykstrom.jcc.common.code.expression;

import se.dykstrom.jcc.common.intermediate.Blank;
import se.dykstrom.jcc.common.intermediate.CodeContainer;
import se.dykstrom.jcc.common.intermediate.Line;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.Type;

import java.util.List;

public class FunctionCallCodeGenerator extends AbstractExpressionCodeGenerator<FunctionCallExpression, TypeManager, AbstractCodeGenerator> {

    public FunctionCallCodeGenerator(final AbstractCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(FunctionCallExpression expression, StorageLocation location) {
        CodeContainer cc = new CodeContainer();

        String name = expression.getIdentifier().name();

        // Get arguments
        List<Expression> args = expression.getArgs();
        // Get types of arguments
        List<Type> argTypes = types().getTypes(args);

        // Get function from symbol table
        Function function = types().resolveFunction(name, argTypes, symbols());

        // Call function
        cc.add(Blank.INSTANCE);
        cc.addAll(codeGenerator.functionCall(function, getComment(expression), args, location));
        cc.add(Blank.INSTANCE);

        return cc.lines();
    }
}
