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

package se.dykstrom.jcc.col.code.llvm.statement;

import se.dykstrom.jcc.col.ast.statement.AliasStatement;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.LlvmComment;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;

import java.util.List;

public class AliasCodeGenerator implements LlvmStatementCodeGenerator<AliasStatement> {

    @Override
    public void toLlvm(final AliasStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        lines.add(new LlvmComment(statement.toString()));
    }
}
