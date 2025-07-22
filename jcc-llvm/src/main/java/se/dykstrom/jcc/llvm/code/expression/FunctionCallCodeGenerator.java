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
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.LlvmFunctions;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.CallOperation;
import se.dykstrom.jcc.llvm.operation.LoadOperation;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class FunctionCallCodeGenerator implements LlvmExpressionCodeGenerator<FunctionCallExpression> {

    private final LlvmCodeGenerator codeGenerator;
    private final LlvmFunctions functions;

    public FunctionCallCodeGenerator(final LlvmCodeGenerator codeGenerator, final LlvmFunctions functions) {
        this.codeGenerator = requireNonNull(codeGenerator);
        this.functions = functions;
    }

    @Override
    public LlvmOperand toLlvm(final FunctionCallExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        final var identifier = expression.getIdentifier();
        final var args = expression.getArgs();
        final var argTypes = codeGenerator.typeManager().getTypes(args);

        // Get function from symbol table
        Function function = codeGenerator.typeManager().resolveFunction(identifier.name(), argTypes, symbolTable);

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
            final var opVariable = new TempOperand("%" + function.getName(), identifier.type());
            final var opTemporary = new TempOperand(symbolTable.nextTempName(), identifier.type());
            // Load the function pointer into a register
            lines.add(new LoadOperation(opTemporary, opVariable));
            // Create a new reference function with the name of the register
            function = rf.withName(opTemporary.name());
        }

        // Evaluate args
        final List<LlvmOperand> opArgs = args.stream()
                .map(arg -> codeGenerator.expression(arg, lines, symbolTable))
                .toList();
        final var type = codeGenerator.typeManager().getType(expression);
        final var opResult = new TempOperand(symbolTable.nextTempName(), type);
        lines.add(new CallOperation(opResult, function, opArgs));
        return opResult;
    }
}
