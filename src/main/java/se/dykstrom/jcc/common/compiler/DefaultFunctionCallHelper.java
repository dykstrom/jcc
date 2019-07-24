/*
 * Copyright (C) 2019 Johan Dykstrom
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
 * The default function call helper class, that generates code for function calls
 * in non-garbage collected languages.
 *
 * @author Johan Dykstrom
 */
public class DefaultFunctionCallHelper implements FunctionCallHelper {

    final CodeGenerator codeGenerator;
    final CodeContainer codeContainer;
    final StorageFactory storageFactory;
    final TypeManager typeManager;

    private final StorageLocation[] intLocations;
    private final StorageLocation[] floatLocations;

    private static final String SHADOW_SPACE = "20h";

    DefaultFunctionCallHelper(CodeGenerator codeGenerator, CodeContainer codeContainer, StorageFactory storageFactory, TypeManager typeManager) {
        this.codeGenerator = codeGenerator;
        this.codeContainer = codeContainer;
        this.storageFactory = storageFactory;
        this.typeManager = typeManager;

        this.intLocations = new StorageLocation[]{ storageFactory.rcx, storageFactory.rdx, storageFactory.r8, storageFactory.r9  };
        this.floatLocations = new StorageLocation[]{ storageFactory.xmm0, storageFactory.xmm1, storageFactory.xmm2, storageFactory.xmm3  };
    }

    @Override
    public void addFunctionCall(Function function, Call functionCall, Comment functionComment, List<Expression> args, StorageLocation returnLocation) {
        List<Expression> expressions = new ArrayList<>(args);

        add(functionComment.withPrefix("--- ").withSuffix(" -->"));

        // Evaluate and remove the first four arguments (if there are so many)
        List<StorageLocation> locations = evaluateRegisterArguments(expressions);

        // Evaluate any extra arguments
        int numberOfPushedArgs = expressions.size();
        evaluateStackArguments(expressions);

        // Move register arguments to function call registers
        if (!locations.isEmpty()) {
            add(new Comment("Move evaluated arguments to argument passing registers"));
            for (int i = 0; i < locations.size(); i++) {
                // For varargs function we don't know the argument type, but it is not needed anyway
                Type formalArgType = function.isVarargs() ? Unknown.INSTANCE : function.getArgTypes().get(i);
                moveArgToRegister(formalArgType, locations.get(i), i, function.isVarargs());
            }
        }

        // Allocate shadow space, call function, and clean up shadow space again
        add(new Comment("Allocate shadow space for call to " + function.getMappedName()));
        add(new SubImmFromReg(SHADOW_SPACE, RSP));
        add(functionCall);
        add(new Comment("Clean up shadow space for call to " + function.getMappedName()));
        add(new AddImmToReg(SHADOW_SPACE, RSP));

        // Save function return value in provided storage location
        moveResultToStorageLocation(function, returnLocation);

        // Clean up pushed arguments
        cleanUpStackArguments(args, numberOfPushedArgs);

        // Clean up register arguments
        cleanUpRegisterArguments(args, locations);

        add(functionComment.withPrefix("<-- ").withSuffix(" ---"));
    }

    /**
     * Generates code for moving the function call result to the given storage location.
     * The result can be found in different registers depending on the type or the return
     * value (RAX or XMM0).
     */
    private void moveResultToStorageLocation(Function function, StorageLocation location) {
        if (function.getReturnType() instanceof F64) {
            add(new Comment("Move result of call (xmm0) to storage location (" + location + ")"));
            location.moveLocToThis(storageFactory.xmm0, codeContainer);
        } else {
            add(new Comment("Move result of call (rax) to storage location (" + location + ")"));
            location.moveLocToThis(storageFactory.rax, codeContainer);
        }
    }

    /**
     * Cleans up pushed arguments by restoring the stack pointer.
     */
    void cleanUpStackArguments(List<Expression> args, int numberOfPushedArgs) {
        if (numberOfPushedArgs > 0) {
            add(new Comment("Clean up " + numberOfPushedArgs + " pushed argument(s)"));
            add(new AddImmToReg(Integer.toString(numberOfPushedArgs * 0x8), RSP));
        }
    }

    /**
     * Cleans up register arguments by closing their storage locations.
     */
    void cleanUpRegisterArguments(List<Expression> args, List<StorageLocation> locations) {
        locations.forEach(StorageLocation::close);
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
                floatLocations[index].convertAndMoveLocToThis(actualArgLocation, codeContainer);
            } else {
                intLocations[index].convertAndMoveLocToThis(actualArgLocation, codeContainer);
            }
        }
    }

    /**
     * Evaluates the rest of the arguments, if there are any, and pushes them on the stack.
     *
     * @param expressions A list of expressions to evaluate.
     */
    private void evaluateStackArguments(List<Expression> expressions) {
        // Check that there actually _are_ extra arguments, before starting to push
        if (!expressions.isEmpty()) {
            add(new Comment("Push " + expressions.size() + " additional argument(s) to stack"));
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
    }

    /**
     * Evaluates the first four arguments that are transferred in registers, if there are so many
     * arguments. This method destructively removes expressions from the given list of expressions
     * after they have been evaluated.
     *
     * @param expressions A list of expressions to evaluate.
     * @return A list of storage locations that contain the results of evaluating the expressions.
     */
    private List<StorageLocation> evaluateRegisterArguments(List<Expression> expressions) {
        List<StorageLocation> locations = new ArrayList<>();
        while (!expressions.isEmpty() && locations.size() < 4) {
            locations.add(evaluateExpression(expressions.remove(0)));
        }
        return locations;
    }

    /**
     * Generates code for evaluating the given expression, storing the result in a newly
     * allocated storage location.
     *
     * @param expression The expression to evaluate.
     * @return The storage location used when evaluating the expression.
     */
    private StorageLocation evaluateExpression(Expression expression) {
        // Find type of expression
        Type type = typeManager.getType(expression);

        // Allocate a new storage location, and evaluate expression
        StorageLocation location = storageFactory.allocateNonVolatile(type);
        codeGenerator.expression(expression, location);
        return location;
    }

    /**
     * Adds the given line of code to the code container.
     */
    void add(Code code) {
        codeContainer.add(code);
    }

    /**
     * Adds the given lines of code to the code container.
     */
    void addAll(List<Code> codes) {
        codeContainer.addAll(codes);
    }
}
