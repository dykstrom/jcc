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

package se.dykstrom.jcc.basic.code.llvm.statement;

import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.llvm.LlvmUtils;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.WhileCodeGenerator;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.BinaryOperation;

import static se.dykstrom.jcc.common.ast.FloatLiteral.FL_F64_0_0;
import static se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO;
import static se.dykstrom.jcc.common.compiler.TypeManager.isFloat;
import static se.dykstrom.jcc.llvm.LlvmOperator.FCMP;
import static se.dykstrom.jcc.llvm.LlvmOperator.ICMP;

public final class BasicWhile {

    private BasicWhile() { }

    public static WhileCodeGenerator.ConditionCodeGenerator conditionCodeGenerator(final LlvmCodeGenerator cg) {
        return (expression, lines, symbolTable) -> {
            final var opLeft = cg.expression(expression, lines, symbolTable);
            final var type = opLeft.type();
            // Choose the zero value, operator, and flag depending on the type.
            // BASIC allows floating point conditions, as well as integer conditions.
            final var zero = isFloat(type) ? FL_F64_0_0 : ZERO;
            final var operator = LlvmUtils.typeToOperator(type, FCMP, ICMP);
            final var flag = isFloat(type) ? "oeq" : "eq";
            final var opZero = cg.expression(zero, lines, symbolTable);
            final var opResult = new TempOperand(symbolTable.nextTempName(), Bool.INSTANCE);
            // Compare the while condition with zero, and return the result.
            // We cannot use the normal EqualExpression here, because the BASIC specialization
            // converts the result to i64, but here we actually need the i1 produced by icmp.
            lines.add(new BinaryOperation(opResult, operator, opLeft, opZero, new String[]{flag}));
            return opResult;
        };
    }
}
