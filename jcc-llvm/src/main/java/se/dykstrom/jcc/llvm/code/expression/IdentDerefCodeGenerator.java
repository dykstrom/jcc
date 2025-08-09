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

package se.dykstrom.jcc.llvm.code.expression;

import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.LoadOperation;

import java.util.List;

public class IdentDerefCodeGenerator implements LlvmExpressionCodeGenerator<IdentifierDerefExpression> {

    @Override
    public LlvmOperand toLlvm(final IdentifierDerefExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        final var identifier = expression.getIdentifier();
        if (symbolTable.contains(identifier.name())) {
            final var address = (String) symbolTable.getValue(identifier.name());
            // Create operands
            final var opVariable = new TempOperand(address, identifier.type());
            final var opResult = new TempOperand(symbolTable.nextTempName(), identifier.type());
            // Load current value
            lines.add(new LoadOperation(opResult, opVariable));
            return opResult;
        } else if (symbolTable.containsFunction(identifier.name())) {
            final var function = symbolTable.getFunction(identifier);
            // Reference to global function prefixed with @
            return new TempOperand("@" + function.mangledName(), identifier.type());
        } else {
            throw new IllegalStateException(identifier.name() + " not found");
        }
    }
}
