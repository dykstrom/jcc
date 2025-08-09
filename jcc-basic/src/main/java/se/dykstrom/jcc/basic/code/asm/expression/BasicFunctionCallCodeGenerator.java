/*
 * Copyright (C) 2025 Johan Dykstrom
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

package se.dykstrom.jcc.basic.code.asm.expression;

import se.dykstrom.jcc.basic.compiler.BasicAsmFunctions;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.code.Blank;
import se.dykstrom.jcc.common.code.CodeContainer;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.expression.FunctionCallCodeGenerator;
import se.dykstrom.jcc.common.compiler.AsmCodeGenerator;
import se.dykstrom.jcc.common.functions.BuiltInFunction;
import se.dykstrom.jcc.common.storage.StorageLocation;

import java.util.List;

import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class BasicFunctionCallCodeGenerator extends FunctionCallCodeGenerator {

    public BasicFunctionCallCodeGenerator(final AsmCodeGenerator codeGenerator) {
        super(codeGenerator);
    }

    @Override
    public List<Line> generate(FunctionCallExpression expression, StorageLocation location) {
        return CodeContainer.withCodeContainer(cc -> {
            final var name = expression.getIdentifier().name();

            // Get arguments
            final var args = expression.getArgs();
            // Get types of arguments
            final var argTypes = types().getTypes(args);
            // Get function definition from symbol table
            var function = types().resolveFunction(name, argTypes, symbols());

            // If this is a built-in function, check if we can inline it
            // Otherwise, get the library function that implements this
            // built-in function
            if (function instanceof BuiltInFunction) {
                final var optionalExpression = BasicAsmFunctions.getInlineExpression(function, args);
                if (optionalExpression.isPresent()) {
                    final var inlineExpression = optionalExpression.get();
                    cc.add(Blank.INSTANCE);
                    cc.addAll(codeGenerator.expression(inlineExpression, location));
                    cc.add(Blank.INSTANCE);
                    return;
                } else {
                    function = BasicAsmFunctions.getLibraryFunction(function);
                }
            }

            // Call function
            cc.add(Blank.INSTANCE);
            cc.addAll(codeGenerator.functionCall(function, getComment(expression), args, location));
            cc.add(Blank.INSTANCE);
        });
    }
}
