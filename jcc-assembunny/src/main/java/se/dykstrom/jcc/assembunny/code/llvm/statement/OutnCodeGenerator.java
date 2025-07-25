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

package se.dykstrom.jcc.assembunny.code.llvm.statement;

import se.dykstrom.jcc.assembunny.ast.OutnStatement;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.CallOperation;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.LF_PRINTF_STR_VAR;
import static se.dykstrom.jcc.llvm.LlvmUtils.getCreateFormatIdentifier;

public class OutnCodeGenerator implements LlvmStatementCodeGenerator<OutnStatement> {

    private final LlvmCodeGenerator codeGenerator;

    public OutnCodeGenerator(final LlvmCodeGenerator codeGenerator) {
        this.codeGenerator = requireNonNull(codeGenerator);
    }

    @Override
    public void toLlvm(final OutnStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        final var expression = statement.getExpression();
        final var expressionType = codeGenerator.typeManager().getType(expression);
        final var opFormat = getOpFormat(symbolTable, expressionType);
        final var opExpression = codeGenerator.expression(expression, lines, symbolTable);
        final var opResult = new TempOperand(symbolTable.nextTempName(), LF_PRINTF_STR_VAR.getReturnType());
        lines.add(new CallOperation(opResult, LF_PRINTF_STR_VAR, List.of(opFormat, opExpression)));
    }

    private static TempOperand getOpFormat(SymbolTable symbolTable, Type expressionType) {
        final var identifier = getCreateFormatIdentifier(expressionType, symbolTable);
        final var address = (String) symbolTable.getValue(identifier.name());
        return new TempOperand(address, identifier.type());
    }
}
