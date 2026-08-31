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
import se.dykstrom.jcc.llvm.code.FixedLabel;
import se.dykstrom.jcc.llvm.code.Label;
import se.dykstrom.jcc.llvm.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.code.Comment;
import se.dykstrom.jcc.llvm.code.LabelStack;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.BranchOperation;
import se.dykstrom.jcc.llvm.operation.PhiOperation;

import java.util.List;

import static java.util.Objects.requireNonNull;

public record IfExpressionCodeGenerator(LlvmCodeGenerator cg, LabelStack labelStack) implements LlvmExpressionCodeGenerator<IfExpression> {

    public IfExpressionCodeGenerator(final LlvmCodeGenerator cg, final LabelStack labelStack) {
        this.cg = requireNonNull(cg);
        this.labelStack = requireNonNull(labelStack);
    }

    @Override
    public LlvmOperand toLlvm(final IfExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        lines.add(new Comment(expression.toString()));

        // Create labels
        Label thenLabel = new FixedLabel(symbolTable.nextLabelName());
        Label elseLabel = new FixedLabel(symbolTable.nextLabelName());
        final var resultLabel = new FixedLabel(symbolTable.nextLabelName());

        // Evaluate boolean condition
        final var opCond = cg.expression(expression.ifExpr(), lines, symbolTable);
        lines.add(new BranchOperation(opCond, thenLabel, elseLabel));
        lines.add(new Comment("Created branch to labels " + thenLabel.getName() + " and " + elseLabel.getName()));

        // Evaluate then expression
        lines.add(thenLabel);
        labelStack.push(thenLabel);
        final var opThen = cg.expression(expression.thenExpr(), lines, symbolTable);
        thenLabel = labelStack.pop();
        lines.add(new Comment("Updating then label to " + thenLabel.getName()));
        lines.add(new BranchOperation(resultLabel.withPred(thenLabel)));
        lines.add(new Comment("Created branch to result label " + resultLabel.getName()));

        // Evaluate else expression
        lines.add(elseLabel);
        labelStack.push(elseLabel);
        final var opElse = cg.expression(expression.elseExpr(), lines, symbolTable);
        elseLabel = labelStack.pop();
        lines.add(new Comment("Updating else label to " + elseLabel.getName()));
        lines.add(new BranchOperation(resultLabel.withPred(elseLabel)));
        lines.add(new Comment("Created branch to result label " + resultLabel.getName()));

        // Select result depending on where we came from using phi operation
        lines.add(resultLabel);
        lines.add(new Comment("Added result label " + resultLabel.getName()));
        final var opResult = new TempOperand(symbolTable.nextTempName(), opThen.type());
        lines.add(new PhiOperation(opResult, List.of(opThen, opElse), List.of(thenLabel, elseLabel)));
        if (labelStack.isNotEmpty()) {
            // If this is not the top-level IF, update the pushed label to this result label
            lines.add(new Comment("Should update latest then/else label to " + resultLabel.getName()));
            labelStack.replace(resultLabel);
        }
        return opResult;
    }
}
