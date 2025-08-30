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

import se.dykstrom.jcc.common.ast.ClsStatement;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Constant;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.CallOperation;

import java.util.List;

import static se.dykstrom.jcc.common.functions.LibcBuiltIns.CF_PRINTF_STR_VAR;

public class ClsCodeGenerator implements LlvmStatementCodeGenerator<ClsStatement> {

    // The string "\u001B" is the Unicode representation of the escape character
    private static final String FORMAT_STR = "\u001B[2J\u001B[H\0";
    private static final String FORMAT_NAME = ".cls.ansi.codes";
    private static final Identifier IDENTIFIER = new Identifier(FORMAT_NAME, Str.INSTANCE);

    @Override
    public void toLlvm(final ClsStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        if (!symbolTable.contains(IDENTIFIER.name())) {
            symbolTable.addConstant(new Constant(IDENTIFIER, FORMAT_STR));
        }
        final var opFormat = new TempOperand(IDENTIFIER.name(), IDENTIFIER.type());
        final var opResult = new TempOperand(symbolTable.nextTempName(), CF_PRINTF_STR_VAR.getReturnType());
        lines.add(new CallOperation(opResult, CF_PRINTF_STR_VAR, List.of(opFormat)));
    }
}
