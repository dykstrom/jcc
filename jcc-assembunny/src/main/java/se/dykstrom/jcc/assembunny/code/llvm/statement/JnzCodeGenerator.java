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

package se.dykstrom.jcc.assembunny.code.llvm.statement;

import se.dykstrom.jcc.assembunny.ast.JnzStatement;
import se.dykstrom.jcc.assembunny.compiler.AssembunnyLlvmCodeGenerator;
import se.dykstrom.jcc.common.code.FixedLabel;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.BinaryOperation;
import se.dykstrom.jcc.llvm.operation.BranchOperation;

import java.util.List;

import static se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO;
import static se.dykstrom.jcc.llvm.LlvmOperator.ICMP;

public class JnzCodeGenerator implements LlvmStatementCodeGenerator<JnzStatement> {

    private final AssembunnyLlvmCodeGenerator codeGenerator;

    public JnzCodeGenerator(AssembunnyLlvmCodeGenerator codeGenerator) {
        this.codeGenerator = codeGenerator;
    }

    public void toLlvm(final JnzStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        final var opExpression = codeGenerator.expression(statement.getExpression(), lines, symbolTable);
        final var opZero = codeGenerator.expression(ZERO, lines, symbolTable);
        final var opCondition = new TempOperand(symbolTable.nextTempName(), Bool.INSTANCE);
        lines.add(new BinaryOperation(opCondition, ICMP, opExpression, opZero, new String[]{"eq"}));
        final var nextLabel = new FixedLabel(symbolTable.nextLabelName());
        lines.add(new BranchOperation(opCondition, nextLabel, new FixedLabel(statement.getTarget())));
        lines.add(nextLabel);
    }
}
