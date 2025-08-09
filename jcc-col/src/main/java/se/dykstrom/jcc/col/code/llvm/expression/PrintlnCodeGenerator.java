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

package se.dykstrom.jcc.col.code.llvm.expression;

import se.dykstrom.jcc.col.ast.expression.PrintlnExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.*;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.LlvmExpressionCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.CallOperation;
import se.dykstrom.jcc.llvm.operation.ConvertOperation;

import java.util.List;

import static se.dykstrom.jcc.common.functions.LibcBuiltIns.CF_PRINTF_STR_VAR;
import static se.dykstrom.jcc.llvm.LlvmOperator.FPEXT;
import static se.dykstrom.jcc.llvm.LlvmOperator.ZEXT;
import static se.dykstrom.jcc.llvm.LlvmUtils.getCreateFormatIdentifier;

public record PrintlnCodeGenerator(LlvmCodeGenerator codeGenerator) implements LlvmExpressionCodeGenerator<PrintlnExpression> {

    @Override
    public LlvmOperand toLlvm(final PrintlnExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        // Get the type of the expression to print
        final var expressionType = codeGenerator.typeManager().getType(expression.getExpression());
        // Get the first argument to printf: the format string
        final var opFormat = getOpFormat(symbolTable, expressionType);
        // Generate code for the expression, and get the temporary operand that contains the result
        final var opExpression = codeGenerator.expression(expression.getExpression(), lines, symbolTable);

        if (expressionType == Bool.INSTANCE) {
            // If the expression is of type bool, we must zero-extend the value to i32 to be able to print it
            final var opExtended = new TempOperand(symbolTable.nextTempName(), I32.INSTANCE);
            lines.add(new ConvertOperation(opExpression, ZEXT, opExtended));
            // Create a temporary operand for the result of calling printf
            final var opResult = new TempOperand(symbolTable.nextTempName(), CF_PRINTF_STR_VAR.getReturnType());
            // Generate code for calling printf with the format string and the zero-extended expression result
            lines.add(new CallOperation(opResult, CF_PRINTF_STR_VAR, List.of(opFormat, opExtended)));
            // Return the result of calling printf
            return opResult;
        }
        if (expressionType == F32.INSTANCE) {
            // If the expression is of type f32, we must extend the value to f64 to be able to print it
            final var opExtended = new TempOperand(symbolTable.nextTempName(), F64.INSTANCE);
            lines.add(new ConvertOperation(opExpression, FPEXT, opExtended));
            // Create a temporary operand for the result of calling printf
            final var opResult = new TempOperand(symbolTable.nextTempName(), CF_PRINTF_STR_VAR.getReturnType());
            // Generate code for calling printf with the format string and the extended expression result
            lines.add(new CallOperation(opResult, CF_PRINTF_STR_VAR, List.of(opFormat, opExtended)));
            // Return the result of calling printf
            return opResult;
        }

        // Create a temporary operand for the result of calling printf
        final var opResult = new TempOperand(symbolTable.nextTempName(), CF_PRINTF_STR_VAR.getReturnType());
        // Generate code for calling printf with the format string and the expression result
        lines.add(new CallOperation(opResult, CF_PRINTF_STR_VAR, List.of(opFormat, opExpression)));
        // Return the result of calling printf
        return opResult;
    }

    private static TempOperand getOpFormat(final SymbolTable symbolTable, final Type expressionType) {
        final var identifier = getCreateFormatIdentifier(expressionType, symbolTable);
        return new TempOperand(identifier.name(), identifier.type());
    }
}
