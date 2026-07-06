/*
 * Copyright (C) 2026 Johan Dykstrom
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

package se.dykstrom.jcc.basic.code.llvm.expression;

import se.dykstrom.jcc.basic.ast.expression.UboundExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Ptr;
import se.dykstrom.jcc.llvm.LlvmComment;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.LlvmExpressionCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LiteralOperand;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.BinaryOperation;
import se.dykstrom.jcc.llvm.operation.GetElementPtrOperation;
import se.dykstrom.jcc.llvm.operation.LoadOperation;

import java.util.List;

import static se.dykstrom.jcc.common.ast.IntegerLiteral.ONE;
import static se.dykstrom.jcc.llvm.LlvmOperator.SUB;
import static se.dykstrom.jcc.llvm.operation.ArrayDimsOperation.DIMS_SUFFIX;

/**
 * Generates code for the inlined 'ubound' function. The upper bound is {@code size(d) - 1},
 * where {@code size(d)} is read from the array's dimension-metadata global (the libjccbas
 * {@code .ubound} runtime function is not used).
 */
public record UboundCodeGenerator(LlvmCodeGenerator cg) implements LlvmExpressionCodeGenerator<UboundExpression> {

    private static final LlvmOperand OP_ONE = new LiteralOperand(ONE.getValue(), ONE.type());

    @Override
    public LlvmOperand toLlvm(final UboundExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        lines.add(new LlvmComment(expression.toString()));
        final var arrayIdentifier = expression.array().getIdentifier();

        // The 1-based dimension; the metadata global is 0-indexed.
        final var opDimension = cg.expression(expression.dimension(), lines, symbolTable);
        final var opIndex = new TempOperand(symbolTable.nextTempName(), I64.INSTANCE);
        lines.add(new BinaryOperation(opIndex, SUB, opDimension, OP_ONE));

        // Load size(d) from the dimension-metadata global, then upper bound = size - 1.
        final var opDims = new TempOperand(symbolTable.mapName(arrayIdentifier) + DIMS_SUFFIX, Ptr.INSTANCE);
        final var opSizePtr = new TempOperand(symbolTable.nextTempName(), I64.INSTANCE);
        lines.add(new GetElementPtrOperation(opSizePtr, opDims, opIndex));
        final var opSize = new TempOperand(symbolTable.nextTempName(), I64.INSTANCE);
        lines.add(new LoadOperation(opSize, opSizePtr));
        final var opResult = new TempOperand(symbolTable.nextTempName(), I64.INSTANCE);
        lines.add(new BinaryOperation(opResult, SUB, opSize, OP_ONE));
        return opResult;
    }
}
