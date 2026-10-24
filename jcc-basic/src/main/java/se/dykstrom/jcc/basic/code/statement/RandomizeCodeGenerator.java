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

import se.dykstrom.jcc.basic.ast.statement.RandomizeStatement;
import se.dykstrom.jcc.common.ast.StringLiteral;
import se.dykstrom.jcc.llvm.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Ptr;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.llvm.code.GcCodeGenerator;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.CallOperation;

import java.util.List;

import static se.dykstrom.jcc.basic.compiler.LibJccBasBuiltIns.JF_RANDOMIZE_F64;
import static se.dykstrom.jcc.basic.compiler.LibJccBasBuiltIns.JF_READ_LINE;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.CF_ATOF_STR;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.CF_PRINTF_STR_VAR;
import static se.dykstrom.jcc.llvm.LlvmUtils.getCreateFormatIdentifier;

public record RandomizeCodeGenerator(LlvmCodeGenerator codeGenerator, GcCodeGenerator gc) implements LlvmStatementCodeGenerator<RandomizeStatement> {

    private static final String PROMPT = "Random Number Seed (-32768 to 32767)? ";

    @Override
    public void toLlvm(final RandomizeStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        final LlvmOperand opSeed;
        if (statement.getExpression() == null) {
            // Print prompt
            final var opPrompt = codeGenerator.expression(StringLiteral.from(statement, PROMPT), lines, symbolTable);
            final var promptFormat = getCreateFormatIdentifier(Str.INSTANCE, symbolTable, false);
            final var opPromptFormat = new TempOperand(symbolTable.mapName(promptFormat), promptFormat.type());
            final var opPromptResult = new TempOperand(symbolTable.nextTempName(), CF_PRINTF_STR_VAR.getReturnType());
            lines.add(new CallOperation(opPromptResult, CF_PRINTF_STR_VAR, List.of(opPromptFormat, opPrompt)));
            // Read user input and convert to seed
            final var opLine = new TempOperand(symbolTable.nextTempName(), Ptr.INSTANCE);
            lines.add(new CallOperation(opLine, JF_READ_LINE, List.of()));
            // read_line returns a freshly malloc'd string; hand it to the collector rather than
            // freeing it. It needs no synthetic slot: atof consumes it immediately, and nothing
            // registers (and so nothing can collect) between here and that call.
            final var opRegistered = gc.register(opLine, lines, symbolTable);
            opSeed = new TempOperand(symbolTable.nextTempName(), CF_ATOF_STR.getReturnType());
            lines.add(new CallOperation((TempOperand) opSeed, CF_ATOF_STR, List.of(opRegistered)));
        } else {
            opSeed = codeGenerator.expression(statement.getExpression(), lines, symbolTable);
        }
        lines.add(new CallOperation(null, JF_RANDOMIZE_F64, List.of(opSeed)));
    }
}
