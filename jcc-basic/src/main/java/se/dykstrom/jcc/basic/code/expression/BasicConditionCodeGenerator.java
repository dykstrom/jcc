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

package se.dykstrom.jcc.basic.code.expression;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.llvm.LlvmUtils;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.LlvmExpressionCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.BinaryOperation;

import java.util.List;

import static se.dykstrom.jcc.common.ast.FloatLiteral.FL_F64_0_0;
import static se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO;
import static se.dykstrom.jcc.llvm.LlvmOperator.FCMP;
import static se.dykstrom.jcc.llvm.LlvmOperator.ICMP;

/**
 * BASIC specific class to use when generating code for IF and WHILE conditions.
 * This class converts a BASIC specific condition to the type and value that is
 * expected by LLVM.
 */
public record BasicConditionCodeGenerator(LlvmCodeGenerator cg) implements LlvmExpressionCodeGenerator<Expression> {

    @Override
    public LlvmOperand toLlvm(Expression expression, List<Line> lines, SymbolTable symbolTable) {
        final var opLeft = cg.expression(expression, lines, symbolTable);
        final var type = opLeft.type();
        // Choose the zero value, operator, and flag depending on the type.
        // BASIC allows floating point conditions, as well as integer conditions.
        final var zero = type.isFloat() ? FL_F64_0_0 : ZERO;
        final var operator = LlvmUtils.typeToOperator(type, FCMP, ICMP);
        final var flag = type.isFloat() ? "one" : "ne";
        final var opZero = cg.expression(zero, lines, symbolTable);
        final var opResult = new TempOperand(symbolTable.nextTempName(), Bool.INSTANCE);
        // Compare the condition with zero, and return the result.
        // We cannot use the normal NotEqualExpression here, because the BASIC specialization
        // converts the result to i64, but here we actually need the i1 produced by icmp.
        lines.add(new BinaryOperation(opResult, operator, opLeft, opZero, new String[]{flag}));
        return opResult;
    }
}
