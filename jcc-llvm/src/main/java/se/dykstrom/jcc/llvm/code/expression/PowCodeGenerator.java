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

import se.dykstrom.jcc.common.ast.PowExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.F64;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.CallOperation;

import java.util.List;

import static se.dykstrom.jcc.llvm.code.LlvmBuiltIns.LF_POW_F32_F32;
import static se.dykstrom.jcc.llvm.code.LlvmBuiltIns.LF_POW_F64_F64;

public record PowCodeGenerator(LlvmCodeGenerator codeGenerator) implements LlvmExpressionCodeGenerator<PowExpression> {

    @Override
    public LlvmOperand toLlvm(final PowExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        final var opLeft = codeGenerator.expression(expression.getLeft(), lines, symbolTable);
        final var opRight = codeGenerator.expression(expression.getRight(), lines, symbolTable);
        final var opResult = new TempOperand(symbolTable.nextTempName(), opLeft.type());
        final var function = (opLeft.type() instanceof F64) ? LF_POW_F64_F64 : LF_POW_F32_F32;
        lines.add(new CallOperation(opResult, function, List.of(opLeft, opRight)));
        return opResult;
    }
}
