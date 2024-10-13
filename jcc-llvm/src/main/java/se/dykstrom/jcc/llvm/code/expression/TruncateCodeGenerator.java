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

import se.dykstrom.jcc.common.ast.TruncateExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.TruncateOperation;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class TruncateCodeGenerator implements LlvmExpressionCodeGenerator<TruncateExpression> {

    private final LlvmCodeGenerator codeGenerator;

    public TruncateCodeGenerator(final LlvmCodeGenerator codeGenerator) {
        this.codeGenerator = requireNonNull(codeGenerator);
    }

    @Override
    public LlvmOperand toLlvm(final TruncateExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        final var type = codeGenerator.typeManager().getType(expression);
        final var opSource = codeGenerator.expression(expression.getExpression(), lines, symbolTable);
        final var opDestination = new TempOperand(symbolTable.nextTempName(), type);
        lines.add(new TruncateOperation(opSource, opDestination));
        return opDestination;
    }
}
