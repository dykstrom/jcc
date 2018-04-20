/*
 * Copyright (C) 2018 Johan Dykstrom
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

import se.dykstrom.jcc.common.assembly.base.Code;
import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Comment;
import se.dykstrom.jcc.common.assembly.instruction.AddImmToReg;
import se.dykstrom.jcc.common.assembly.instruction.Call;
import se.dykstrom.jcc.common.assembly.instruction.SubImmFromReg;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.storage.FloatRegisterStorageLocation;
import se.dykstrom.jcc.common.storage.StorageFactory;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.Type;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.assembly.base.Register.RSP;

/**
 * A helper class that generates code for function calls.
 *
 * @author Johan Dykstrom
 */
public class FunctionCallHelper {

    private final StorageLocation[] intLocations;
    private final StorageLocation[] floatLocations;

    private final CodeGenerator codeGenerator;
    private final CodeContainer codeContainer;
    private final StorageFactory storageFactory;
    private final TypeManager typeManager;

    FunctionCallHelper(CodeGenerator codeGenerator, CodeContainer codeContainer, StorageFactory storageFactory, TypeManager typeManager) {
        this.codeGenerator = codeGenerator;
        this.codeContainer = codeContainer;
        this.storageFactory = storageFactory;
        this.typeManager = typeManager;

        this.intLocations = new StorageLocation[]{ storageFactory.rcx, storageFactory.rdx, storageFactory.r8, storageFactory.r9  };
        this.floatLocations = new StorageLocation[]{ storageFactory.xmm0, storageFactory.xmm1, storageFactory.xmm2, storageFactory.xmm3  };
    }

    /**
     * Generates code for making the given {@code functionCall}. The list of expressions is evaluated, and the
     * values are stored in the function call registers (RCX, RDX, R8, and R9 for integer and pointer arguments,
     * or XMM0, XMM1, XMM2, and XMM3 for floating point arguments) and on the stack if needed.
     *
     * Shadow space is also allocated and cleaned up if needed. The already allocated {@link StorageLocation}
     * given to this method is used as the first storage location when evaluating the function argument
     * expressions. If more storage locations are required, they are allocated and de-allocated inside the
     * method.
     *
     * @param functionCall The function call to make.
     * @param functionComment A function call comment to insert before calling the function.
     * @param isVarargs True if the called function is a varargs function.
     * @param args The arguments to the function.
     * @param firstLocation An already allocated storage location to use when evaluating expressions.
     */
    public void addFunctionCall(Call functionCall, Comment functionComment, boolean isVarargs, List<Expression> args, StorageLocation firstLocation) {
        List<Expression> expressions = new ArrayList<>(args);

        // Evaluate first argument
        if (!expressions.isEmpty()) {
            expression(expressions.remove(0), firstLocation);
        }

        // Evaluate the next three arguments (if there are so many)
        List<StorageLocation> storedArgs = new ArrayList<>(singletonList(firstLocation));
        while (!expressions.isEmpty() && storedArgs.size() < 4) {
            storedArgs.add(removeAndEvaluateFirstExpression(expressions));
        }

        // Evaluate any extra arguments
        int numberOfPushedArgs = 0;
        // Check that there actually _are_ extra arguments
        if (!expressions.isEmpty()) {
            // Push arguments in reverse order
            for (int i = expressions.size() - 1; i >= 0; i--) {
                Expression expression = expressions.get(i);
                Type type = typeManager.getType(expression);
                try (StorageLocation location = storageFactory.allocateNonVolatile(type)) {
                    expression(expression, location);
                    location.pushThis(codeContainer);
                    numberOfPushedArgs++;
                }
            }
        }

        // Move register arguments to function call registers
        if (!storedArgs.isEmpty()) {
            addCode(new Comment("Move evaluated arguments to argument passing registers"));
            for (int i = 0; i < storedArgs.size(); i++) {
                moveArgToRegister(storedArgs.get(i), i, isVarargs);
            }
        }

        // Clean up register arguments (except the first one, that was allocated elsewhere)
        for (int i = 1; i < storedArgs.size(); i++) {
            storedArgs.get(i).close();
        }

        // If any args were pushed on the stack, we must allocate new shadow space before calling the function
        // Otherwise, we let the called function reuse the shadow space of this function?
        if (numberOfPushedArgs > 0) {
            addCode(new Comment("Allocate shadow space"));
            addCode(new SubImmFromReg(Integer.toString(0x20), RSP));
        }
        addCode(functionComment);
        addCode(functionCall);
        // If any args were pushed on the stack, we must consequently clean up the stack after the call
        if (numberOfPushedArgs > 0) {
            // Calculate size of shadow space plus pushed args that must be popped
            Integer stackSpace = 0x20 + numberOfPushedArgs * 0x8;
            addCode(new Comment("Clean up shadow space and " + numberOfPushedArgs + " pushed arg(s)"));
            addCode(new AddImmToReg(stackSpace.toString(), RSP));
        }
    }

    /**
     * Generates code for moving the result of evaluating the argument in {@code storedArg}
     * to the corresponding argument passing register.
     *
     * @param storedArg Result of evaluating an argument.
     * @param index The index of the argument to move.
     * @param isVarargs True if the called function is a varargs function.
     */
    private void moveArgToRegister(StorageLocation storedArg, int index, boolean isVarargs) {
        if (storedArg instanceof FloatRegisterStorageLocation) {
            floatLocations[index].moveLocToThis(storedArg, codeContainer);
            // Varargs functions require floating point arguments to be duplicated in both XMM registers,
            // and general purpose registers
            if (isVarargs) {
                intLocations[index].moveLocToThis(storedArg, codeContainer);
            }
        } else {
            intLocations[index].moveLocToThis(storedArg, codeContainer);
        }
    }

    /**
     * Processes the first expression in the list by removing it from the expression list, evaluating it,
     * and returning its storage location.
     */
    private StorageLocation removeAndEvaluateFirstExpression(List<Expression> expressions) {
        // Find type of first expression
        Type type = typeManager.getType(expressions.get(0));

        StorageLocation location = storageFactory.allocateNonVolatile(type);
        expression(expressions.remove(0), location);
        return location;
    }

    private void expression(Expression expression, StorageLocation location) {
        codeGenerator.expression(expression, location);
    }

    private void addCode(Code code) {
        codeContainer.add(code);
    }
}
