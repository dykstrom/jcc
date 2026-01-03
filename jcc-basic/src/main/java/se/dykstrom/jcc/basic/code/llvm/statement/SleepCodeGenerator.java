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

import se.dykstrom.jcc.basic.ast.statement.SleepStatement;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operation.CallOperation;

import java.util.List;

import static se.dykstrom.jcc.basic.compiler.LibJccBasBuiltIns.JF_SLEEP_F64;
import static se.dykstrom.jcc.common.ast.FloatLiteral.FL_F64_0_0;

public record SleepCodeGenerator(LlvmCodeGenerator codeGenerator) implements LlvmStatementCodeGenerator<SleepStatement> {

    @Override
    public void toLlvm(final SleepStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        final LlvmOperand opSeconds;
        if (statement.getExpression() == null) {
            opSeconds = codeGenerator.expression(FL_F64_0_0, lines, symbolTable);
        } else {
            opSeconds = codeGenerator.expression(statement.getExpression(), lines, symbolTable);
        }
        lines.add(new CallOperation(null, JF_SLEEP_F64, List.of(opSeconds)));
    }
}
