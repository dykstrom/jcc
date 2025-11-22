/*
 * Copyright (C) 2024 Johan Dykstrom
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

package se.dykstrom.jcc.llvm.code.statement;

import se.dykstrom.jcc.common.ast.LabelledStatement;
import se.dykstrom.jcc.common.code.Label;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.operation.BranchOperation;
import se.dykstrom.jcc.llvm.operation.IndirectBranchOperation;

import java.util.List;

public record LabelledCodeGenerator(LlvmCodeGenerator lcg) implements LlvmStatementCodeGenerator<LabelledStatement> {

    @Override
    public void toLlvm(final LabelledStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        // Make sure the basic block before this label ends with a branch operation
        if (!endsWithBranch(lines)) {
            lines.add(new BranchOperation(new Label(statement.label())));
        }
        lines.add(new Label(statement.label()));
        lcg.statement(statement.statement(), lines, symbolTable);
    }

    private static boolean endsWithBranch(final List<Line> lines) {
        if (lines.isEmpty()) {
            return false;
        }
        final var last = lines.getLast();
        return last instanceof BranchOperation || last instanceof IndirectBranchOperation;
    }
}
