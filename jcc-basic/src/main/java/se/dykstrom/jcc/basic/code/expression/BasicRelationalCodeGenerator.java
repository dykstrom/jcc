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

import se.dykstrom.jcc.common.ast.BinaryExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.LlvmExpressionCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.RelationalCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.BinaryOperation;
import se.dykstrom.jcc.llvm.operation.CallOperation;
import se.dykstrom.jcc.llvm.operation.ConvertOperation;

import java.util.List;

import static se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.CF_STRCMP_STR_STR;
import static se.dykstrom.jcc.llvm.LlvmOperator.*;

public record BasicRelationalCodeGenerator(LlvmCodeGenerator lcg, RelationalCodeGenerator rcg)
        implements LlvmExpressionCodeGenerator<BinaryExpression> {

    @Override
    public LlvmOperand toLlvm(final BinaryExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        // Compare strings using strcmp, and numbers using the default code generator
        final var opResult = isString(expression.getLeft()) ?
                strcmp(expression, lines, symbolTable) :
                rcg.toLlvm(expression, lines, symbolTable);
        // QB does not have booleans. The relational operators return a value of type "LONG INTEGER",
        // that is, a 32-bit integer. QB also represents TRUE with -1 instead of 1. So to convert an
        // LLVM boolean to QB we zero extend the i1 to i32 (actually i64) and negate it.
        final var opExtended = new TempOperand(symbolTable.nextTempName(), I64.INSTANCE);
        lines.add(new ConvertOperation(opResult, ZEXT, opExtended));
        final var opZero = lcg.expression(ZERO.withType(opExtended.type()), lines, symbolTable);
        final var opNegated = new TempOperand(symbolTable.nextTempName(), opExtended.type());
        lines.add(new BinaryOperation(opNegated, SUB, opZero, opExtended));
        return opNegated;
    }

    private boolean isString(final Expression expression) {
        return lcg.typeManager().getType(expression) instanceof Str;
    }

    private LlvmOperand strcmp(final BinaryExpression e, final List<Line> lines, final SymbolTable symbolTable) {
        // Call strcmp
        final var opLeft = lcg.expression(e.getLeft(), lines, symbolTable);
        final var opRight = lcg.expression(e.getRight(), lines, symbolTable);
        final var opCompare = new TempOperand(symbolTable.nextTempName(), CF_STRCMP_STR_STR.getReturnType());
        lines.add(new CallOperation(opCompare, CF_STRCMP_STR_STR, List.of(opLeft, opRight)));
        // Compare result with 0
        final var opResult = new TempOperand(symbolTable.nextTempName(), Bool.INSTANCE);
        final var opZero = lcg.expression(ZERO, lines, symbolTable);
        lines.add(new BinaryOperation(opResult, ICMP, opCompare, opZero, new String[]{rcg.iFlag()}));
        return opResult;
    }
}
