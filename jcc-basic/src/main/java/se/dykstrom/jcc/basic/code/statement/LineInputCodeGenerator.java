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

import se.dykstrom.jcc.basic.ast.statement.LineInputStatement;
import se.dykstrom.jcc.common.ast.StringLiteral;
import se.dykstrom.jcc.llvm.code.Line;
import se.dykstrom.jcc.common.symbols.Scope;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Ptr;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.llvm.code.GcCodeGenerator;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.CallOperation;
import se.dykstrom.jcc.llvm.operation.StoreOperation;

import java.util.List;

import static se.dykstrom.jcc.basic.compiler.LibJccBasBuiltIns.JF_READ_LINE;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.CF_PRINTF_STR_VAR;
import static se.dykstrom.jcc.llvm.LlvmUtils.getCreateFormatIdentifier;

public record LineInputCodeGenerator(LlvmCodeGenerator cg, GcCodeGenerator gc, Scope scope) implements LlvmStatementCodeGenerator<LineInputStatement> {

    @Override
    public void toLlvm(final LineInputStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        // Add variable to symbol table
        final Identifier identifier = statement.identifier();
        if (!symbolTable.contains(identifier.name())) {
            switch (scope) {
                case GLOBAL -> symbolTable.addGlobal(identifier, identifier.type().llvmDefaultValue());
                case LOCAL -> symbolTable.addVariable(identifier, identifier.type().llvmDefaultValue());
                case NONE -> throw new IllegalStateException(identifier.name() + " not found");
            }
        }

        // Print prompt if required
        if (statement.prompt() != null) {
            final var opPrompt = cg.expression(StringLiteral.from(statement, statement.prompt()), lines, symbolTable);
            final var promptFormat = getCreateFormatIdentifier(Str.INSTANCE, symbolTable, false);
            final var opPromptFormat = new TempOperand(symbolTable.mapName(promptFormat), promptFormat.type());
            final var opPromptResult = new TempOperand(symbolTable.nextTempName(), CF_PRINTF_STR_VAR.getReturnType());
            lines.add(new CallOperation(opPromptResult, CF_PRINTF_STR_VAR, List.of(opPromptFormat, opPrompt)));
        }

        final TempOperand opResult = new TempOperand(symbolTable.nextTempName(), Ptr.INSTANCE);
        lines.add(new CallOperation(opResult, JF_READ_LINE, List.of()));

        // read_line returns a freshly malloc'd string; hand it to the collector. No synthetic
        // slot is needed because it is stored immediately into the destination variable, which
        // is itself a registered root.
        final var opRegistered = gc.register(opResult, lines, symbolTable);

        // Assign result to identifier
        final var opDestination = new TempOperand(symbolTable.mapName(identifier), identifier.type());
        // Store new value
        lines.add(new StoreOperation(opRegistered, opDestination));

        // No newline is printed after reading input. When stdin is an interactive
        // terminal the newline is already echoed by the terminal, so emitting one
        // here would produce a blank line. As a result the inhibitNewline flag has
        // no effect.
    }
}
