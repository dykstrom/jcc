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

package se.dykstrom.jcc.basic.code.statement;

import se.dykstrom.jcc.basic.ast.statement.OptionBaseStatement;
import se.dykstrom.jcc.basic.compiler.BasicCodeGenerator;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.LlvmComment;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Handles the OPTION BASE statement. The base only affects the value returned
 * by LBOUND (the array lower bound), which is computed inline at compile time, so no runtime call
 * is emitted here — the base is merely recorded on the code generator for LBOUND to read. OPTION
 * BASE never affects element indexing, which is always zero-based.
 */
public record OptionBaseCodeGenerator(BasicCodeGenerator cg) implements LlvmStatementCodeGenerator<OptionBaseStatement> {

    public OptionBaseCodeGenerator {
        requireNonNull(cg);
    }

    @Override
    public void toLlvm(final OptionBaseStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        lines.add(new LlvmComment(statement.toString()));
        cg.setOptionBase(statement.base());
    }
}
