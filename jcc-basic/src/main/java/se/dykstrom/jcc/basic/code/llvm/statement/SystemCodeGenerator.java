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

package se.dykstrom.jcc.basic.code.llvm.statement;

import se.dykstrom.jcc.basic.ast.statement.SystemStatement;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.LlvmComment;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LiteralOperand;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.CallOperation;

import java.util.List;

import static se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.CF_EXIT_I64;

public record SystemCodeGenerator() implements LlvmStatementCodeGenerator<SystemStatement> {

    private static final LlvmOperand OP_ZERO = new LiteralOperand(ZERO.getValue(), ZERO.type());

    @Override
    public void toLlvm(final SystemStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        final var opResult = new TempOperand(symbolTable.nextTempName(), CF_EXIT_I64.getReturnType());
        lines.add(new LlvmComment("Exit program"));
        lines.add(new CallOperation(opResult, CF_EXIT_I64, List.of(OP_ZERO)));
    }
}
