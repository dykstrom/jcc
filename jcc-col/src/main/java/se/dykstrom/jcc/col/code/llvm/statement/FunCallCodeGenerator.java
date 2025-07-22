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

import se.dykstrom.jcc.col.ast.statement.FunCallStatement;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class FunCallCodeGenerator  implements LlvmStatementCodeGenerator<FunCallStatement> {

    private final LlvmCodeGenerator codeGenerator;

    public FunCallCodeGenerator(final LlvmCodeGenerator codeGenerator) {
        this.codeGenerator = requireNonNull(codeGenerator);
    }

    @Override
    public void toLlvm(FunCallStatement statement, List<Line> lines, SymbolTable symbolTable) {
        final var funCallExpression = statement.expression();
        codeGenerator.expression(funCallExpression, lines, symbolTable);
    }
}
