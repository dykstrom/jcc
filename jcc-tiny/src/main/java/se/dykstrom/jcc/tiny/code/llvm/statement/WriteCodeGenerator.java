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

package se.dykstrom.jcc.tiny.code.llvm.statement;

import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Constant;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.CallOperation;
import se.dykstrom.jcc.tiny.ast.WriteStatement;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.FUN_PRINTF_STR_VAR;

public class WriteCodeGenerator implements LlvmStatementCodeGenerator<WriteStatement> {

    private final LlvmCodeGenerator codeGenerator;

    public WriteCodeGenerator(final LlvmCodeGenerator codeGenerator) {
        this.codeGenerator = requireNonNull(codeGenerator);
    }

    public void toLlvm(final WriteStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        statement.getExpressions().forEach(e -> {
            final var expressionType = codeGenerator.typeManager().getType(e);
            final var identifier = getCreateFormatIdentifier(expressionType, symbolTable);
            final var address = (String) symbolTable.getValue(identifier.name());
            final var opFormat = new TempOperand(address, identifier.type());
            final var opExpression = codeGenerator.expression(e, lines, symbolTable);
            final var opResult = new TempOperand(symbolTable.nextTempName(), FUN_PRINTF_STR_VAR.getReturnType());
            lines.add(new CallOperation(opResult, FUN_PRINTF_STR_VAR, List.of(opFormat, opExpression)));
        });
    }

    private static Identifier getCreateFormatIdentifier(final Type type, final SymbolTable symbolTable) {
        final var formatStr = type.getFormat() + "\n\0";
        final var formatName = ".printf.fmt." + type;
        final var identifier = new Identifier(formatName, Str.INSTANCE);
        if (!symbolTable.contains(formatName)) {
            final var globalIdentifier = new Identifier("@" + formatName, Str.INSTANCE);
            // A global string constant is represented by two entries in the symbol table.
            // The first links the identifier to the global "address" of the constant.
            symbolTable.addConstant(new Constant(identifier, globalIdentifier.name()));
            // The second links the global address to the actual string value.
            symbolTable.addConstant(new Constant(globalIdentifier, formatStr));
        }
        return identifier;
    }
}
