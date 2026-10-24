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
import se.dykstrom.jcc.llvm.code.Label;
import se.dykstrom.jcc.llvm.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;

import java.util.List;

import static se.dykstrom.jcc.llvm.LlvmUtils.addBranchIfNeeded;

public record LabelledCodeGenerator(LlvmCodeGenerator lcg) implements LlvmStatementCodeGenerator<LabelledStatement> {

    @Override
    public void toLlvm(final LabelledStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        final var label = new Label(statement.label());
        // Make sure the basic block before this label ends with a branch operation
        addBranchIfNeeded(lines, label);
        lines.add(label);
        lcg.statement(statement.statement(), lines, symbolTable);
    }
}
