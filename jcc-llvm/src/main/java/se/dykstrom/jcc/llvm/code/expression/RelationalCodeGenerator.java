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

import se.dykstrom.jcc.common.ast.BinaryExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.llvm.LlvmUtils;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.BinaryOperation;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.llvm.LlvmOperator.FCMP;
import static se.dykstrom.jcc.llvm.LlvmOperator.ICMP;

public class RelationalCodeGenerator implements LlvmExpressionCodeGenerator<BinaryExpression> {

    private final LlvmCodeGenerator codeGenerator;
    private final String fFlag;
    private final String iFlag;

    public RelationalCodeGenerator(final LlvmCodeGenerator codeGenerator,
                                   final String fFlag,
                                   final String iFlag) {
        this.codeGenerator = requireNonNull(codeGenerator);
        this.fFlag = requireNonNull(fFlag);
        this.iFlag = requireNonNull(iFlag);
    }

    @Override
    public LlvmOperand toLlvm(final BinaryExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        final var opLeft = codeGenerator.expression(expression.getLeft(), lines, symbolTable);
        final var opRight = codeGenerator.expression(expression.getRight(), lines, symbolTable);
        // Get type from left subexpression, since the type of the relational expression is Bool
        final var type = codeGenerator.typeManager().getType(expression.getLeft());
        final var operator = LlvmUtils.typeToOperator(type, FCMP, ICMP);
        final var flag = codeGenerator.typeManager().isFloat(type) ? fFlag : iFlag;
        final var opResult = new TempOperand(symbolTable.nextTempName(), Bool.INSTANCE);
        lines.add(new BinaryOperation(opResult, operator, opLeft, opRight, new String[]{flag}));
        return opResult;
    }
}
