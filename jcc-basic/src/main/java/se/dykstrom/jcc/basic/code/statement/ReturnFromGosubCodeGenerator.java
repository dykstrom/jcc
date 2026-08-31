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

package se.dykstrom.jcc.basic.code.statement;

import se.dykstrom.jcc.basic.ast.statement.ReturnFromGosubStatement;
import se.dykstrom.jcc.llvm.code.Label;
import se.dykstrom.jcc.llvm.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Ptr;
import se.dykstrom.jcc.llvm.LlvmComment;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.CallOperation;
import se.dykstrom.jcc.llvm.operation.IndirectBranchOperation;

import java.util.List;

import static se.dykstrom.jcc.basic.compiler.LibJccBasBuiltIns.JF_GOSUB_POP;

public record ReturnFromGosubCodeGenerator(List<Label> possibleReturnTargets)
        implements LlvmStatementCodeGenerator<ReturnFromGosubStatement> {

    @Override
    public void toLlvm(final ReturnFromGosubStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        final var opReturnAddress = new TempOperand(symbolTable.nextTempName(), Ptr.INSTANCE);
        lines.add(new LlvmComment("Pop the return address"));
        lines.add(new CallOperation(opReturnAddress, JF_GOSUB_POP, List.of()));
        lines.add(new LlvmComment("Jump back to caller - must list ALL possible return targets"));
        lines.add(new IndirectBranchOperation(opReturnAddress, possibleReturnTargets));
    }
}
