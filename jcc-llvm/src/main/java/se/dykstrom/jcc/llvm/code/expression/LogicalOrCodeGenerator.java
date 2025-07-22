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

package se.dykstrom.jcc.llvm.code.expression;

import se.dykstrom.jcc.common.ast.LogicalOrExpression;
import se.dykstrom.jcc.common.code.FixedLabel;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.BranchOperation;
import se.dykstrom.jcc.llvm.operation.PhiOperation;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.common.ast.BooleanLiteral.TRUE;

public class LogicalOrCodeGenerator implements LlvmExpressionCodeGenerator<LogicalOrExpression> {

    private final LlvmCodeGenerator codeGenerator;

    public LogicalOrCodeGenerator(final LlvmCodeGenerator codeGenerator) {
        this.codeGenerator = requireNonNull(codeGenerator);
    }

    @Override
    public LlvmOperand toLlvm(final LogicalOrExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        // Create labels
        final var leftLabel = new FixedLabel(symbolTable.nextLabelName());
        final var rightLabel = new FixedLabel(symbolTable.nextLabelName());
        final var resultLabel = new FixedLabel(symbolTable.nextLabelName());

        // Branch to left label so we can use it in the phi operation
        lines.add(new BranchOperation(leftLabel));

        // Evaluate left expression
        lines.add(leftLabel);
        final var opLeft = codeGenerator.expression(expression.getLeft(), lines, symbolTable);
        lines.add(new BranchOperation(opLeft, resultLabel, rightLabel));

        // Evaluate right expression
        lines.add(rightLabel);
        final var opRight = codeGenerator.expression(expression.getRight(), lines, symbolTable);
        lines.add(new BranchOperation(resultLabel));

        // Select result depending on where we came from using phi operation
        lines.add(resultLabel);
        final var opResult = new TempOperand(symbolTable.nextTempName(), Bool.INSTANCE);
        final var opTrue = codeGenerator.expression(TRUE, lines, symbolTable);
        // If we came directly from the left label, the result is always true
        // Otherwise, the result is equal to the result of the right expression
        lines.add(new PhiOperation(opResult, List.of(opTrue, opRight), List.of(leftLabel, rightLabel)));
        return opResult;
    }
}
