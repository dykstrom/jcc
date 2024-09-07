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

import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.assembly.instruction.AddImmToReg;
import se.dykstrom.jcc.common.assembly.instruction.Call;
import se.dykstrom.jcc.common.assembly.instruction.SubImmFromReg;
import se.dykstrom.jcc.common.ast.*;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.code.CodeContainer;
import se.dykstrom.jcc.common.code.Comment;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.storage.FloatRegisterStorageLocation;
import se.dykstrom.jcc.common.storage.RegisterStorageLocation;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.common.types.Parameter;
import se.dykstrom.jcc.common.types.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static se.dykstrom.jcc.common.assembly.base.FloatRegister.*;
import static se.dykstrom.jcc.common.assembly.base.Register.*;
import static se.dykstrom.jcc.common.utils.ExpressionUtils.hasNoUdfFunctionCall;

/**
 * The default function call helper class, that generates code for function calls
 * in non-garbage collected languages.
 *
 * @author Johan Dykstrom
 */
public class DefaultFunctionCallHelper implements FunctionCallHelper {

    private static final String SHADOW_SPACE = "20h";

    protected final CodeGenerator codeGenerator;

    DefaultFunctionCallHelper(final CodeGenerator codeGenerator) {
        this.codeGenerator = codeGenerator;
    }

    @Override
    public List<Line> addFunctionCall(final Function function,
                                      final Call functionCall,
                                      final Comment functionComment,
                                      final List<Expression> args,
                                      final StorageLocation returnLocation) {
        CodeContainer cc = new CodeContainer();

        cc.add(functionComment.withPrefix("--- ").withSuffix(" -->"));

        // Evaluate the first four arguments (if there are so many)
        if (!args.isEmpty()) {
            cc.add(new AssemblyComment("Evaluate arguments (" + function.getMappedName() + ")"));
        }
        final var locations = evaluateRegisterArguments(args, cc);

        // Evaluate any extra arguments and push them on the stack
        evaluateStackArguments(args, cc);

        // Move register arguments to function call registers
        if (!locations.isEmpty()) {
            cc.add(new AssemblyComment("Move arguments to argument passing registers (" + function.getMappedName() + ")"));
            moveRegisterArguments(function, args, locations, cc);
        }

        // Allocate shadow space, call function, and clean up shadow space again
        cc.add(new AssemblyComment("Allocate shadow space (" + function.getMappedName() + ")"));
        cc.add(new SubImmFromReg(SHADOW_SPACE, RSP));
        cc.add(functionCall);
        cc.add(new AssemblyComment("Clean up shadow space (" + function.getMappedName() + ")"));
        cc.add(new AddImmToReg(SHADOW_SPACE, RSP));

        // Save function return value in provided storage location
        moveResultToStorageLocation(function, returnLocation, cc);

        // Clean up pushed arguments
        cleanUpStackArguments(args, Math.max(args.size() - 4, 0), cc);

        // Clean up register arguments
        cleanUpRegisterArguments(args, locations, cc);

        cc.add(functionComment.withPrefix("<-- ").withSuffix(" ---"));

        return cc.lines();
    }

    /**
     * Generates code for moving the function call result to the given storage location.
     * The result can be found in different registers depending on the type or the return
     * value (RAX or XMM0).
     */
    private void moveResultToStorageLocation(Function function, StorageLocation location, CodeContainer cc) {
        if (location == null) {
            cc.add(new AssemblyComment("Ignore return value"));
        } else {
            if (function.getReturnType() instanceof F64) {
                cc.add(new AssemblyComment("Move return value (xmm0) to storage location (" + location + ")"));
                location.moveLocToThis(codeGenerator.storageFactory().get(XMM0), cc);
            } else {
                cc.add(new AssemblyComment("Move return value (rax) to storage location (" + location + ")"));
                location.moveLocToThis(codeGenerator.storageFactory().get(RAX), cc);
            }
        }
    }

    /**
     * Cleans up pushed arguments by restoring the stack pointer.
     */
    void cleanUpStackArguments(List<Expression> args, int numberOfPushedArgs, CodeContainer cc) {
        if (numberOfPushedArgs > 0) {
            cc.add(new AssemblyComment(String.format("Clean up %d pushed argument(s)", numberOfPushedArgs)));
            cc.add(new AddImmToReg(String.format("%xh", numberOfPushedArgs * 0x8), RSP));
        }
    }

    /**
     * Cleans up register arguments by closing their storage locations. Note that if the evaluation
     * of a register argument was deferred, the corresponding item in the list of locations is null.
     */
    void cleanUpRegisterArguments(List<Expression> args, List<StorageLocation> locations, CodeContainer cc) {
        locations.stream().filter(Objects::nonNull).forEach(StorageLocation::close);
    }

    private void moveRegisterArguments(final Function function,
                                       final List<Expression> args,
                                       final List<StorageLocation> locations,
                                       final CodeContainer cc) {
        for (int i = 0; i < locations.size(); i++) {
            // For varargs function we don't know the argument type, but it is not needed anyway
            final var formalArgType = function.isVarargs() ? null : function.getArgTypes().get(i);
            if (locations.get(i) == null) {
                performDeferredEvaluation(formalArgType, args.get(i), i, function.isVarargs(), cc);
            } else {
                moveArgToRegister(formalArgType, locations.get(i), i, function.isVarargs(), cc);
            }
        }
    }

    /**
     * Generates code for evaluating a simple argument that did not need to be evaluated
     * in the correct order related to its position in the argument list. We have already
     * checked that this is ok, so here we just do the evaluation using the right argument
     * passing register.
     */
    private void performDeferredEvaluation(final Type formalArgType,
                                           final Expression expression,
                                           final int index,
                                           final boolean isVarargs,
                                           final CodeContainer cc) {
        if (isVarargs) {
            // We don't know the formal argument types for varargs functions, but they require all
            // arguments to be stored in general purpose registers, and floating point arguments
            // to be stored in the XMM registers as well
            final var type = codeGenerator.types().getType(expression);
            if (type instanceof F64) {
                final var floatLocation = getFloatLocation(index);
                // Use the float register to evaluate the expression,
                // and copy the result also to the corresponding g.p. register
                cc.addAll(codeGenerator.expression(expression, floatLocation));
                getIntLocation(index).moveLocToThis(floatLocation, cc);
            } else {
                cc.addAll(codeGenerator.expression(expression, getIntLocation(index)));
            }
        } else {
            // For non-varargs functions we use the type of the formal argument
            // to determine which argument passing register to use
            final StorageLocation location;
            if (formalArgType instanceof F64) {
                location = getFloatLocation(index);
            } else {
                location = getIntLocation(index);
            }
            cc.addAll(codeGenerator.expression(expression, location));
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
     * @param cc The code container.
     */
    private void moveArgToRegister(final Type formalArgType,
                                   final StorageLocation actualArgLocation,
                                   final int index,
                                   final boolean isVarargs,
                                   final CodeContainer cc) {
        if (isVarargs) {
            // We don't know the formal argument types for varargs functions, but they require all
            // arguments to be stored in general purpose registers, and floating point arguments
            // to be stored in the XMM registers as well
            getIntLocation(index).moveLocToThis(actualArgLocation, cc);
            // We cannot check the formalArgType here, because it is null
            if (actualArgLocation instanceof FloatRegisterStorageLocation) {
                getFloatLocation(index).moveLocToThis(actualArgLocation, cc);
            }
        } else {
            // For non-varargs functions we use the type of the formal argument
            // to determine which argument passing register to use
            if (formalArgType instanceof F64) {
                getFloatLocation(index).convertAndMoveLocToThis(actualArgLocation, cc);
            } else {
                getIntLocation(index).convertAndMoveLocToThis(actualArgLocation, cc);
            }
        }
    }

    /**
     * Evaluates the rest of the arguments, if there are any, and pushes them on the stack.
     * The first four arguments are ignored, because they are transferred via registers.
     *
     * @param expressions A list of expressions to evaluate.
     * @param cc The code container.
     */
    private void evaluateStackArguments(final List<Expression> expressions, final CodeContainer cc) {
        // Check that there actually _are_ extra arguments, before starting to push
        if (expressions.size() > 4) {
            cc.add(new AssemblyComment("Push " + (expressions.size() - 4) + " additional argument(s) to stack"));

            // Evaluate arguments left to right
            final var locations = new ArrayList<StorageLocation>();
            for (int i = 4; i < expressions.size(); i++) {
                final var expression = expressions.get(i);
                if (canBeEvaluatedLater(i, expressions, codeGenerator.symbols())) {
                    cc.add(new AssemblyComment("Defer evaluation of argument " + i + ": " + expression));
                    locations.add(null);
                } else {
                    locations.add(evaluateExpression(expression, cc));
                }
            }

            // Push arguments right to left
            for (int i = locations.size() - 1; i >= 0; i--) {
                final var location = locations.get(i);
                if (location == null) {
                    final var expression = expressions.get(i + 4);
                    final var type = codeGenerator.types().getType(expression);
                    // Literal values and variables should be pushed directly to the stack
                    // instead of using a temporary location and some extra instructions
                    try (StorageLocation tempLocation = codeGenerator.storageFactory().allocateNonVolatile(type)) {
                        cc.addAll(codeGenerator.expression(expression, tempLocation));
                        tempLocation.pushThis(cc);
                    }
                } else {
                    location.pushThis(cc);
                    location.close();
                }
            }
        }
    }

    /**
     * Evaluates the first four arguments that are transferred in registers, if there are so many
     * arguments.
     *
     * @param expressions A list of expressions to evaluate.
     * @param cc The code container.
     * @return A list of storage locations that contain the results of evaluating the expressions.
     */
    private List<StorageLocation> evaluateRegisterArguments(final List<Expression> expressions,
                                                            final CodeContainer cc) {
        final List<StorageLocation> locations = new ArrayList<>();
        for (int i = 0; i < expressions.size() && i < 4; i++) {
            final var expression = expressions.get(i);
            if (canBeEvaluatedLater(i, expressions, codeGenerator.symbols())) {
                cc.add(new AssemblyComment("Defer evaluation of argument " + i + ": " + expression));
                locations.add(null);
            } else {
                locations.add(evaluateExpression(expression, cc));
            }
        }
        return locations;
    }

    /**
     * Returns true if the specified argument can be evaluated "later" and does not need
     * to be evaluated in the correct order and stored in a temporary location. This applies
     * to all literal values, and can also apply to variables, in case the variable cannot
     * be modified before actually calling the function.
     */
    public static boolean canBeEvaluatedLater(final int index,
                                              final List<Expression> args,
                                              final SymbolTable symbols) {
        final var expression = args.get(index);
        if (expression instanceof LiteralExpression) {
            // Literal values can always be evaluated later
            return true;
        }
        if (expression instanceof ArrayAccessExpression) {
            // Access to array elements cannot be evaluated later
            return false;
        }
        if (expression instanceof IdentifierNameExpression) {
            // References to identifier names, i.e. addresses, can be evaluated later
            return true;
        }
        if (expression instanceof IdentifierDerefExpression ide) {
            // Variable dereferences can be evaluated later under some circumstances:
            // - If there are no function calls to user-defined function later in the argument list
            //   (a user-defined function may change the value of a global variable)
            // - If the variable is a local variable or a function parameter
            //   (a user-defined function cannot change the value of those (but a closure could))
            if (ide.getIdentifier() instanceof Parameter) {
                // Parameters can be evaluated later
                return true;
            }
            // Check if there is a UDF call later in the argument list
            final var rest = args.subList(index + 1, args.size());
            return rest.stream().allMatch(e -> hasNoUdfFunctionCall(e, symbols));
        }
        return false;
    }

    /**
     * Generates code for evaluating the given expression, storing the result in a newly
     * allocated storage location.
     *
     * @param expression The expression to evaluate.
     * @param cc The code container.
     * @return The storage location used when evaluating the expression.
     */
    private StorageLocation evaluateExpression(Expression expression, CodeContainer cc) {
        // Find type of expression
        Type type = codeGenerator.types().getType(expression);

        // Allocate a new storage location, and evaluate expression
        StorageLocation location = codeGenerator.storageFactory().allocateNonVolatile(type);
        cc.addAll(codeGenerator.expression(expression, location));
        return location;
    }

    /**
     * Returns the {@link FloatRegisterStorageLocation} used for argument number {@code index}.
     */
    private FloatRegisterStorageLocation getFloatLocation(final int index) {
        return switch (index) {
            case 0 -> codeGenerator.storageFactory().get(XMM0);
            case 1 -> codeGenerator.storageFactory().get(XMM1);
            case 2 -> codeGenerator.storageFactory().get(XMM2);
            case 3 -> codeGenerator.storageFactory().get(XMM3);
            default -> throw new IllegalStateException("Unexpected value: " + index);
        };
    }

    /**
     * Returns the {@link RegisterStorageLocation} used for argument number {@code index}.
     */
    private RegisterStorageLocation getIntLocation(final int index) {
        return switch (index) {
            case 0 -> codeGenerator.storageFactory().get(RCX);
            case 1 -> codeGenerator.storageFactory().get(RDX);
            case 2 -> codeGenerator.storageFactory().get(R8);
            case 3 -> codeGenerator.storageFactory().get(R9);
            default -> throw new IllegalStateException("Unexpected value: " + index);
        };
    }
}
