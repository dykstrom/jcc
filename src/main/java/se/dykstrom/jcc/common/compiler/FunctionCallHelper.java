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
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.storage.FloatRegisterStorageLocation;
import se.dykstrom.jcc.common.storage.StorageFactory;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.common.types.Unknown;

import java.util.ArrayList;
import java.util.List;

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
     * @param function The function to call.
     * @param functionCall The function call to make.
     * @param functionComment A function call comment to insert before calling the function.
     * @param args The arguments to the function.
     * @param firstLocation An already allocated storage location to use when evaluating expressions.
     */
    public void addFunctionCall(Function function, Call functionCall, Comment functionComment, List<Expression> args, StorageLocation firstLocation) {
        List<Expression> expressions = new ArrayList<>(args);

        // Evaluate the next three arguments (if there are so many)
        List<StorageLocation> locations = new ArrayList<>();
        while (!expressions.isEmpty() && locations.size() < 4) {
            // If we have not yet used firstLocation, try to use that if possible
            StorageLocation location = locations.contains(firstLocation) ? null : firstLocation;
            locations.add(evaluateExpression(expressions.remove(0), location));
        }

        // Evaluate any extra arguments
        int numberOfPushedArgs = expressions.size();
        // Check that there actually _are_ extra arguments, before starting to push
        if (!expressions.isEmpty()) {
            addCode(new Comment("Push " + numberOfPushedArgs + " additional arguments to stack"));
            // Push arguments in reverse order
            for (int i = expressions.size() - 1; i >= 0; i--) {
                Expression expression = expressions.get(i);
                Type type = typeManager.getType(expression);
                try (StorageLocation location = storageFactory.allocateNonVolatile(type)) {
                    codeGenerator.expression(expression, location);
                    location.pushThis(codeContainer);
                }
            }
        }

        // Move register arguments to function call registers
        if (!locations.isEmpty()) {
            addCode(new Comment("Move evaluated arguments to argument passing registers"));
            for (int i = 0; i < locations.size(); i++) {
                // For varargs function we don't know the argument type, but it is not needed anyway
                Type formalArgType = function.isVarargs() ? Unknown.INSTANCE : function.getArgTypes().get(i);
                moveArgToRegister(formalArgType, locations.get(i), i, function.isVarargs());
            }
        }

        // Clean up register arguments (except firstLocation, that was allocated elsewhere)
        locations.stream().filter(location -> !location.equals(firstLocation)).forEach(StorageLocation::close);

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
     * Generates code for moving the result of evaluating the argument from
     * {@code actualArgLocation} to the correct argument passing register.
     *
     * @param formalArgType The type of the formal argument.
     * @param actualArgLocation Stores the result of evaluating the actual argument.
     * @param index The index of the argument in the argument list.
     * @param isVarargs True if the called function is a varargs function.
     */
    private void moveArgToRegister(Type formalArgType, StorageLocation actualArgLocation, int index, boolean isVarargs) {
        if (isVarargs) {
            // We don't know the formal argument types for varargs functions, but they require all
            // arguments to be stored in general purpose registers, and floating point arguments
            // to be stored in the XMM registers as well
            intLocations[index].moveLocToThis(actualArgLocation, codeContainer);
            if (actualArgLocation instanceof FloatRegisterStorageLocation) {
                floatLocations[index].moveLocToThis(actualArgLocation, codeContainer);
            }
        } else {
            // For non-varargs functions we use the type of the formal argument
            // to determine which argument passing register to use
            if (formalArgType instanceof F64) {
                floatLocations[index].moveLocToThis(actualArgLocation, codeContainer);
            } else {
                intLocations[index].moveLocToThis(actualArgLocation, codeContainer);
            }
        }
    }

    /**
     * Generates code for evaluating the given expression, storing the result in the given
     * storage location if possible. If the storage location is not available, or cannot
     * store values of this type, a new storage location is allocated. The method returns
     * the storage location actually used.
     *
     * @param expression The expression to evaluate.
     * @param loc The storage location to use if possible. If {@code null} then a new storage location will be allocated.
     * @return The storage location used when evaluating the expression.
     */
    private StorageLocation evaluateExpression(Expression expression, StorageLocation loc) {
        // Find type of expression
        Type type = typeManager.getType(expression);

        // Use loc if possible, otherwise allocate a new location
        StorageLocation location = (loc != null && loc.stores(type)) ? loc : storageFactory.allocateNonVolatile(type);
        codeGenerator.expression(expression, location);
        return location;
    }

    private void addCode(Code code) {
        codeContainer.add(code);
    }
}
