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

package se.dykstrom.jcc.basic.code.expression;

import se.dykstrom.jcc.basic.ast.expression.AscExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.I8;
import se.dykstrom.jcc.llvm.LlvmComment;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.LlvmExpressionCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LiteralOperand;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.ConvertOperation;
import se.dykstrom.jcc.llvm.operation.GetElementPtrOperation;
import se.dykstrom.jcc.llvm.operation.LoadOperation;

import java.util.List;

import static se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO;
import static se.dykstrom.jcc.llvm.LlvmOperator.ZEXT;

/**
 * Generates code for the inlined 'asc' function.
 */
public record AscCodeGenerator(LlvmCodeGenerator cg) implements LlvmExpressionCodeGenerator<AscExpression> {

    private static final LlvmOperand OP_ZERO = new LiteralOperand(ZERO.getValue(), ZERO.type());

    @Override
    public LlvmOperand toLlvm(final AscExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        lines.add(new LlvmComment(expression.toString()));

        // Get the address of the first character in the string
        final var opString = cg.expression(expression.expression(), lines, symbolTable);
        final var opAddress = new TempOperand(symbolTable.nextTempName(), I8.INSTANCE);
        lines.add(new GetElementPtrOperation(opAddress, opString, OP_ZERO));

        // Load the character from the address as an i8
        final var opChar = new TempOperand(symbolTable.nextTempName(), I8.INSTANCE);
        lines.add(new LoadOperation(opChar, opAddress));

        // Extend the character ASCII value from i8 to i64
        final var opExtended = new TempOperand(symbolTable.nextTempName(), I64.INSTANCE);
        lines.add(new ConvertOperation(opChar, ZEXT, opExtended));

        return opExtended;
    }
}
