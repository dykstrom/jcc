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

import se.dykstrom.jcc.basic.ast.statement.LineInputStatement;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.Scope;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Ptr;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.CallOperation;
import se.dykstrom.jcc.llvm.operation.StoreOperation;

import java.util.List;

import static se.dykstrom.jcc.basic.compiler.LibJccBasBuiltIns.JF_READ_LINE;

public record LineInputCodeGenerator(LlvmCodeGenerator cg, Scope scope) implements LlvmStatementCodeGenerator<LineInputStatement> {

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
            // TODO: Print prompt.
        }

        final TempOperand opResult = new TempOperand(symbolTable.nextTempName(), Ptr.INSTANCE);
        lines.add(new CallOperation(opResult, JF_READ_LINE, List.of()));

        // Assign result to identifier
        final var opDestination = new TempOperand(symbolTable.mapName(identifier), identifier.type());
        // Store new value
        lines.add(new StoreOperation(opResult, opDestination));

        // TODO: Register dynamic memory.
    }
}
