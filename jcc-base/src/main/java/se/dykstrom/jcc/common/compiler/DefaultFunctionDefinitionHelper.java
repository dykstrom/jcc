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

package se.dykstrom.jcc.common.compiler;

import java.util.List;
import java.util.Map;
import java.util.Set;

import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.assembly.instruction.Ret;
import se.dykstrom.jcc.common.assembly.other.Snippets;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.functions.UserDefinedFunction;
import se.dykstrom.jcc.common.intermediate.Blank;
import se.dykstrom.jcc.common.intermediate.CodeContainer;
import se.dykstrom.jcc.common.intermediate.Line;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.Parameter;
import se.dykstrom.jcc.common.types.Str;

import static se.dykstrom.jcc.common.assembly.base.FloatRegister.XMM0;
import static se.dykstrom.jcc.common.assembly.base.Register.RAX;
import static se.dykstrom.jcc.common.assembly.base.Register.RBP;
import static se.dykstrom.jcc.common.assembly.base.Register.RCX;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_STRDUP;
import static se.dykstrom.jcc.common.functions.FunctionUtils.LIB_LIBC;
import static se.dykstrom.jcc.common.functions.MemoryManagementUtils.allocatesDynamicMemory;

public class DefaultFunctionDefinitionHelper implements FunctionDefinitionHelper {

    private final CodeGenerator codeGenerator;

    public DefaultFunctionDefinitionHelper(final CodeGenerator codeGenerator) {
        this.codeGenerator = codeGenerator;
    }

    @Override
    public List<Line> addFunctionCode(final UserDefinedFunction function, final Expression expression) {
        return codeGenerator.withLocalSymbolTable(() -> {
            // Add formal arguments to local symbol table
            // Note: We only support scalar arguments for now
            final var argNames = function.argNames();
            final var argTypes = function.getArgTypes();
            for (int i = 0; i < argNames.size(); i++) {
                codeGenerator.symbols().addParameter(new Parameter(
                        argNames.get(i),
                        argTypes.get(i),
                        String.format("%s+%xh", RBP, 0x10 + i * 0x8)
                ));
            }

            final var cc = new CodeContainer();

            cc.add(Blank.INSTANCE);
            cc.add(new AssemblyComment("Definition of: " + function));

            // Add label for start of function
            cc.add(new Label(function.getMappedName()));

            // Save arguments in home locations
            cc.addAll(Snippets.enter(argTypes));

            // Generate assembly code for the actual function code, including saving and restoring used registers
            cc.addAll(codeGenerator.withLocalStorageFactory(lcc -> lcc.addAll(generate(function, expression))));

            // Restore stack
            cc.addAll(Snippets.leave());
            cc.add(new Ret());

            return cc.lines();
        });
    }

    private List<Line> generate(final UserDefinedFunction function, final Expression expression) {
        final var cc = new CodeContainer();

        final var returnType = function.getReturnType();
        try (var resultLocation = codeGenerator.storageFactory().allocateNonVolatile(returnType)) {
            // Generate code for expression
            cc.addAll(codeGenerator.expression(expression, resultLocation));

            // Save result
            if (returnType instanceof F64) {
                // If the return type is float, move the result to XMM0
                cc.add(new AssemblyComment("Move result (" + resultLocation + ") to return value (xmm0)"));
                codeGenerator.storageFactory().get(XMM0).moveLocToThis(resultLocation, cc);
            } else if ((returnType instanceof Str) && !allocatesDynamicMemory(expression, returnType)) {
                // If the return type is string, and the expression we want to return does not cause
                // allocation of memory in itself, we have to duplicate the string so the returned
                // value has its own memory. The caller assumes that a function that returns a string
                // allocates memory.
                cc.add(new AssemblyComment("Allocate memory for return value of type string in " + resultLocation));
                codeGenerator.storageFactory().get(RCX).moveLocToThis(resultLocation, cc);
                cc.addAll(Snippets.strdup(RCX));
                // The result of calling strdup, which is the result we want to return, is already in RAX
                cc.add(new AssemblyComment("Result already in rax"));
                codeGenerator.addAllFunctionDependencies(Map.of(LIB_LIBC, Set.of(FUN_STRDUP)));
            } else {
                // If the return type is integer, or string with memory, just move the result to RAX
                cc.add(new AssemblyComment("Move result (" + resultLocation + ") to return value (rax)"));
                codeGenerator.storageFactory().get(RAX).moveLocToThis(resultLocation, cc);
            }
        }

        return cc.lines();
    }
}
