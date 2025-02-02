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
import se.dykstrom.jcc.common.types.*;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.CallOperation;
import se.dykstrom.jcc.tiny.ast.ReadStatement;

import java.util.List;

import static se.dykstrom.jcc.common.functions.LibcBuiltIns.FUN_SCANF_STR_VAR;

public class ReadCodeGenerator implements LlvmStatementCodeGenerator<ReadStatement> {

    public void toLlvm(final ReadStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        statement.getIdentifiers().forEach(destinationIdentifier -> {
            final var destinationAddress = "%" + destinationIdentifier.name();
            // If the identifier is undefined, add it to the symbol table now
            if (!symbolTable.contains(destinationIdentifier.name())) {
                symbolTable.addVariable(destinationIdentifier, destinationAddress);
            }
            final var formatIdentifier = getCreateFormatIdentifier(destinationIdentifier.type(), symbolTable);
            final var formatAddress = (String) symbolTable.getValue(formatIdentifier.name());
            final var opFormat = new TempOperand(formatAddress, formatIdentifier.type());
            final var opDestination = new TempOperand(destinationAddress, Ptr.INSTANCE);
            final var opResult = new TempOperand(symbolTable.nextTempName(), FUN_SCANF_STR_VAR.getReturnType());
            lines.add(new CallOperation(opResult, FUN_SCANF_STR_VAR, List.of(opFormat, opDestination)));
        });
    }

    private static Identifier getCreateFormatIdentifier(final Type type, final SymbolTable symbolTable) {
        final var formatStr = type.getFormat() + "\0";
        final var formatName = ".scanf.fmt." + type;
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
