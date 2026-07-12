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

package se.dykstrom.jcc.llvm.code.expression;

import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.functions.BuiltInFunction;
import se.dykstrom.jcc.common.functions.Function;
import se.dykstrom.jcc.common.functions.ReferenceFunction;
import se.dykstrom.jcc.common.functions.UserDefinedFunction;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.llvm.LlvmComment;
import se.dykstrom.jcc.llvm.code.GcCodeGenerator;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.LlvmFunctions;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.CallOperation;
import se.dykstrom.jcc.llvm.operation.LoadOperation;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.common.utils.MemoryManagementUtils.allocatesDynamicMemory;

public class FunctionCallCodeGenerator implements LlvmExpressionCodeGenerator<FunctionCallExpression> {

    private final LlvmCodeGenerator codeGenerator;
    private final LlvmFunctions functions;
    private final GcCodeGenerator gc;

    public FunctionCallCodeGenerator(final LlvmCodeGenerator codeGenerator,
                                     final LlvmFunctions functions,
                                     final GcCodeGenerator gc) {
        this.codeGenerator = requireNonNull(codeGenerator);
        this.functions = functions;
        this.gc = requireNonNull(gc);
    }

    @Override
    public LlvmOperand toLlvm(final FunctionCallExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        final var identifier = expression.getIdentifier();
        final var args = expression.getArgs();

        // Get function from expression
        Function function = expression.function();

        // If this is a built-in function, check if we can inline it
        // Otherwise, get the library function that implements this
        // built-in function
        if (function instanceof BuiltInFunction) {
            final var optionalExpression = functions.getInlineExpression(function, args);
            if (optionalExpression.isPresent()) {
                final var inlineExpression = optionalExpression.get();
                return codeGenerator.expression(inlineExpression, lines, symbolTable);
            } else {
                function = functions.getLibraryFunction(function);
            }
        }

        // If this is a reference function, we must load it from the
        // variable into a temporary register to call it
        if (function instanceof ReferenceFunction rf) {
            final var opVariable = new TempOperand(symbolTable.mapName(identifier), identifier.type());
            final var opTemporary = new TempOperand(symbolTable.nextTempName(), identifier.type());
            // Load the function pointer into a register
            lines.add(new LoadOperation(opTemporary, opVariable));
            // Create a new reference function with the name of the register
            function = rf.withName(opTemporary.name());
        }

        // Evaluate args
        lines.add(new LlvmComment(expression.toString()));
        final List<LlvmOperand> opArgs = args.stream()
                .map(arg -> codeGenerator.expression(arg, lines, symbolTable))
                .toList();
        final var type = codeGenerator.typeManager().getType(expression);
        final var opResult = new TempOperand(symbolTable.nextTempName(), type);
        lines.add(new CallOperation(opResult, function, opArgs));

        // Hand a dynamically-allocated string result to the collector so it is neither leaked
        // nor prematurely freed. Arguments need no handling: a string-producing argument was
        // already registered and rooted by its own code generator, so it stays reachable across
        // the call - which is exactly what makes a callee that stashes or returns an argument
        // safe (issue #63, requirement 4). The old post-call argument free is gone.
        if (type instanceof Str) {
            // A user-defined function registers its own result inside the callee (it may even
            // return an argument or a literal it does not own), so its result is only rooted
            // here. A built-in/library function just malloc'd a fresh block, so its result is
            // registered here.
            final var producer = expression.function();
            if (producer instanceof UserDefinedFunction || producer instanceof ReferenceFunction) {
                return gc.protectResult(opResult, lines, symbolTable);
            }
            return gc.registerResult(opResult, lines, symbolTable);
        }
        return opResult;
    }

    /**
     * Generates a guaranteed tail call ({@code musttail}) of the given expression, which must be a
     * direct call to a user-defined function (enforced by semantic analysis). The caller is
     * responsible for emitting a {@code ret} of the returned operand immediately afterward, as
     * {@code musttail} requires.
     *
     * <p>A {@code musttail} call cannot be followed by any post-call GC plumbing (the frame pop
     * must happen before the call), so this method asserts that no argument allocates dynamic
     * memory. For COL's scalar types this never happens; pop-before-become for string arguments
     * is issue #63 phase 7.
     */
    public LlvmOperand toLlvmTailCall(final FunctionCallExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        final var function = expression.function();
        final var args = expression.getArgs();

        lines.add(new LlvmComment("become " + expression));
        final List<LlvmOperand> opArgs = args.stream()
                .map(arg -> codeGenerator.expression(arg, lines, symbolTable))
                .toList();

        for (int i = 0; i < args.size(); i++) {
            if (allocatesDynamicMemory(args.get(i), opArgs.get(i).type())) {
                throw new IllegalStateException(
                        "a musttail (become) call cannot manage dynamically allocated argument memory around the call");
            }
        }

        final var type = codeGenerator.typeManager().getType(expression);
        final var opResult = new TempOperand(symbolTable.nextTempName(), type);
        lines.add(new CallOperation(opResult, function, opArgs, true));
        return opResult;
    }
}
