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

import se.dykstrom.jcc.basic.ast.statement.PrintStatement;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.CallOperation;

import java.util.ArrayList;
import java.util.List;

import static se.dykstrom.jcc.common.functions.LibcBuiltIns.CF_PRINTF_STR_VAR;
import static se.dykstrom.jcc.llvm.LlvmUtils.getCreateFormatIdentifier;

public record PrintCodeGenerator(LlvmCodeGenerator codeGenerator) implements LlvmStatementCodeGenerator<PrintStatement> {

    @Override
    public void toLlvm(final PrintStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        final var opExpressions = statement.getExpressions().stream()
                .map(e -> codeGenerator.expression(e, lines, symbolTable))
                .toList();
        final var opFormat = getOpFormat(opExpressions.stream().map(LlvmOperand::type).toList(), symbolTable);

        final var opResult = new TempOperand(symbolTable.nextTempName(), CF_PRINTF_STR_VAR.getReturnType());
        final var args = new ArrayList<>(opExpressions);
        args.addFirst(opFormat);
        lines.add(new CallOperation(opResult, CF_PRINTF_STR_VAR, args));
    }

    private static TempOperand getOpFormat(final List<Type> types, final SymbolTable symbolTable) {
        final var identifier = getCreateFormatIdentifier(types, symbolTable);
        return new TempOperand(identifier.name(), identifier.type());
    }
}
