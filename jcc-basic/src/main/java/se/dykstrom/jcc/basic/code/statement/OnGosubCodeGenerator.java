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

import se.dykstrom.jcc.basic.ast.statement.OnGosubStatement;
import se.dykstrom.jcc.common.ast.IntegerLiteral;
import se.dykstrom.jcc.common.code.FixedLabel;
import se.dykstrom.jcc.common.code.Label;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.common.types.Ptr;
import se.dykstrom.jcc.llvm.LlvmComment;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LiteralOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.BinaryOperation;
import se.dykstrom.jcc.llvm.operation.BranchOperation;
import se.dykstrom.jcc.llvm.operation.CallOperation;

import java.util.List;

import static se.dykstrom.jcc.basic.compiler.LibJccBasBuiltIns.JF_GOSUB_PUSH_PTR;
import static se.dykstrom.jcc.llvm.LlvmOperator.ICMP;

public record OnGosubCodeGenerator(LlvmCodeGenerator cg) implements LlvmStatementCodeGenerator<OnGosubStatement> {

    @Override
    public void toLlvm(final OnGosubStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        lines.add(new LlvmComment("Evaluate ON-GOSUB expression"));
        final var opExpression = cg.expression(statement.getExpression(), lines, symbolTable);
        final var functionName = symbolTable.getCurrentFunction();
        final var nextLabel = new Label(statement.nextLabel());
        final var opBlockAddress = new LiteralOperand("blockaddress(@" + functionName + ", %" + nextLabel.getMappedName() + ")", Ptr.INSTANCE);

        for (int index = 0; index < statement.getJumpLabels().size(); index++) {
            // BASIC is 1-indexed
            final var bi = index + 1;
            final var literal = new IntegerLiteral(0, 0, bi, opExpression.type());
            final var opIndex = cg.expression(literal, lines, symbolTable);

            final var opResult = new TempOperand(symbolTable.nextTempName(), Bool.INSTANCE);
            lines.add(new LlvmComment("Compare " + opExpression.toText() + " with " + bi));
            lines.add(new BinaryOperation(opResult, ICMP, opExpression, opIndex, new String[]{"eq"}));

            final var equalLabel = new FixedLabel(symbolTable.nextLabelName());
            final var notEqualLabel = new FixedLabel(symbolTable.nextLabelName());
            lines.add(new BranchOperation(opResult, equalLabel, notEqualLabel));

            lines.add(equalLabel);
            lines.add(new LlvmComment("Push the return address (next statement)"));
            lines.add(new CallOperation(null, JF_GOSUB_PUSH_PTR, List.of(opBlockAddress)));
            lines.add(new LlvmComment("Jump to the subroutine"));
            lines.add(new BranchOperation(new Label(statement.getJumpLabels().get(index))));

            lines.add(notEqualLabel);
        }
    }
}
