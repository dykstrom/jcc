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

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.llvm.code.Line;
import se.dykstrom.jcc.common.symbols.Scope;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.llvm.LlvmOperator;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.BinaryOperation;
import se.dykstrom.jcc.llvm.operation.LoadOperation;
import se.dykstrom.jcc.llvm.operation.StoreOperation;

import java.util.List;

public abstract class AbstractOpAssignCodeGenerator {

    private final LlvmCodeGenerator codeGenerator;
    private final Scope scope;

    public AbstractOpAssignCodeGenerator(final LlvmCodeGenerator codeGenerator, final Scope scope) {
        this.codeGenerator = codeGenerator;
        this.scope = scope;
    }

    protected void toLlvm(final Identifier identifier,
                          final List<Line> lines,
                          final SymbolTable symbolTable,
                          final LlvmOperator operator,
                          final Expression expression) {
        if (!symbolTable.contains(identifier.name())) {
            // In a language with undeclared variables, this statement may be
            // the first use of the variable, just like in AssignCodeGenerator
            switch (scope) {
                case GLOBAL -> symbolTable.addGlobal(identifier, identifier.type().llvmDefaultValue());
                case LOCAL -> symbolTable.addVariable(identifier, identifier.type().llvmDefaultValue());
                case NONE -> throw new IllegalStateException(identifier.name() + " not found");
            }
        }
        // Create operands
        final var opVariable = new TempOperand(symbolTable.mapName(identifier), identifier.type());
        final var opLeft = new TempOperand(symbolTable.nextTempName(), identifier.type());
        final var opRight = codeGenerator.expression(expression, lines, symbolTable);
        final var opResult = new TempOperand(symbolTable.nextTempName(), identifier.type());
        // Load current value
        lines.add(new LoadOperation(opLeft, opVariable));
        // Perform operation
        lines.add(new BinaryOperation(opResult, operator, opLeft, opRight));
        // Store updated value
        lines.add(new StoreOperation(opResult, opVariable));
    }
}
