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

import se.dykstrom.jcc.common.ast.ReturnStatement;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.operation.ReturnOperation;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class ReturnCodeGenerator implements LlvmStatementCodeGenerator<ReturnStatement> {

    private final LlvmCodeGenerator codeGenerator;

    public ReturnCodeGenerator(final LlvmCodeGenerator codeGenerator) {
        this.codeGenerator = requireNonNull(codeGenerator);
    }

    @Override
    public void toLlvm(final ReturnStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        if (statement.getExpression() != null) {
            final var opExpression = codeGenerator.expression(statement.getExpression(), lines, symbolTable);
            lines.add(new ReturnOperation(opExpression));
        } else {
            lines.add(new ReturnOperation());
        }
    }
}
