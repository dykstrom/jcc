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

import se.dykstrom.jcc.common.ast.IfExpression;
import se.dykstrom.jcc.common.code.FixedLabel;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.LlvmComment;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.BranchOperation;
import se.dykstrom.jcc.llvm.operation.PhiOperation;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class IfCodeGenerator implements LlvmExpressionCodeGenerator<IfExpression> {

    private final LlvmCodeGenerator codeGenerator;

    public IfCodeGenerator(final LlvmCodeGenerator codeGenerator) {
        this.codeGenerator = requireNonNull(codeGenerator);
    }

    @Override
    public LlvmOperand toLlvm(final IfExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        lines.add(new LlvmComment(expression.toString()));

        // Create labels
        final var thenLabel = new FixedLabel(symbolTable.nextLabelName());
        final var elseLabel = new FixedLabel(symbolTable.nextLabelName());
        final var resultLabel = new FixedLabel(symbolTable.nextLabelName());

        // Evaluate boolean condition
        final var opCond = codeGenerator.expression(expression.ifExpr(), lines, symbolTable);
        lines.add(new BranchOperation(opCond, thenLabel, elseLabel));

        // Evaluate then expression
        lines.add(thenLabel);
        final var opThen = codeGenerator.expression(expression.thenExpr(), lines, symbolTable);
        lines.add(new BranchOperation(resultLabel));

        // Evaluate else expression
        lines.add(elseLabel);
        final var opElse = codeGenerator.expression(expression.elseExpr(), lines, symbolTable);
        lines.add(new BranchOperation(resultLabel));

        // Select result depending on where we came from using phi operation
        lines.add(resultLabel);
        final var opResult = new TempOperand(symbolTable.nextTempName(), opThen.type());
        lines.add(new PhiOperation(opResult, List.of(opThen, opElse), List.of(thenLabel, elseLabel)));
        return opResult;
    }
}
