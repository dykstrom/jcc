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

package se.dykstrom.jcc.llvm.code.statement;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.IfStatement;
import se.dykstrom.jcc.common.code.FixedLabel;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.LlvmComment;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.LlvmExpressionCodeGenerator;
import se.dykstrom.jcc.llvm.operation.BranchOperation;

import java.util.List;

public record IfCodeGenerator(LlvmCodeGenerator cg, LlvmExpressionCodeGenerator<Expression> conditionCodeGenerator)
        implements LlvmStatementCodeGenerator<IfStatement> {

    public IfCodeGenerator(final LlvmCodeGenerator cg) {
        this(cg, cg::expression);
    }

    @Override
    public void toLlvm(final IfStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        // Create labels
        final var beforeLabel = new FixedLabel(symbolTable.nextLabelName());
        final var thenLabel = new FixedLabel(symbolTable.nextLabelName());
        final var elseLabel = new FixedLabel(symbolTable.nextLabelName());
        final var afterLabel = new FixedLabel(symbolTable.nextLabelName());

        // Make sure the basic block before this label ends with a branch operation
        if (!endsWithBranch(lines)) {
            lines.add(new BranchOperation(beforeLabel));
        }
        lines.add(new LlvmComment(statement.toString()));

        // Before if
        lines.add(beforeLabel);
        // Evaluate condition in a language-dependent way
        final var opCond = conditionCodeGenerator.toLlvm(statement.getExpression(), lines, symbolTable);
        lines.add(new BranchOperation(opCond, thenLabel, elseLabel));

        // Then clause
        lines.add(thenLabel);
        statement.getThenStatements().forEach(s -> cg.statement(s, lines, symbolTable));
        lines.add(new BranchOperation(afterLabel));

        // Else clause
        lines.add(elseLabel);
        statement.getElseStatements().forEach(s -> cg.statement(s, lines, symbolTable));
        lines.add(new BranchOperation(afterLabel));

        // After if
        lines.add(new LlvmComment("END IF"));
        lines.add(afterLabel);
    }

    private static boolean endsWithBranch(final List<Line> lines) {
        return !lines.isEmpty() && (lines.getLast() instanceof BranchOperation);
    }
}
