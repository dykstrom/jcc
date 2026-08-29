/*
 * Copyright (C) 2026 Johan Dykstrom
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

package se.dykstrom.jcc.col.code.expression;

import se.dykstrom.jcc.common.ast.BinaryExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.LlvmExpressionCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.RelationalCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.BinaryOperation;
import se.dykstrom.jcc.llvm.operation.CallOperation;

import java.util.List;

import static se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO_I32;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.CF_STRCMP_STR_STR;
import static se.dykstrom.jcc.llvm.LlvmOperator.ICMP;

/**
 * COL specific class that supports comparing strings. Numbers are compared by the default code
 * generator; two strings are compared by content, with {@code strcmp}. Only equality and
 * inequality are wired to this class - COL rejects ordered comparison of strings in semantic
 * analysis ({@code ColOperandTypeRules.NOT_STRINGS}).
 * <p>
 * Nothing is registered with the garbage collector here: {@code strcmp} allocates nothing and
 * returns no view of either operand.
 */
public record ColRelationalCodeGenerator(LlvmCodeGenerator lcg, RelationalCodeGenerator rcg)
        implements LlvmExpressionCodeGenerator<BinaryExpression> {

    @Override
    public LlvmOperand toLlvm(final BinaryExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        return isString(expression.getLeft()) ?
                strcmp(expression, lines, symbolTable) :
                rcg.toLlvm(expression, lines, symbolTable);
    }

    private boolean isString(final Expression expression) {
        return lcg.typeManager().getType(expression) instanceof Str;
    }

    private LlvmOperand strcmp(final BinaryExpression e, final List<Line> lines, final SymbolTable symbolTable) {
        final var opLeft = lcg.expression(e.getLeft(), lines, symbolTable);
        final var opRight = lcg.expression(e.getRight(), lines, symbolTable);
        final var opCompare = new TempOperand(symbolTable.nextTempName(), CF_STRCMP_STR_STR.getReturnType());
        lines.add(new CallOperation(opCompare, CF_STRCMP_STR_STR, List.of(opLeft, opRight)));
        // strcmp returns zero for equal strings, so the wrapped generator's integer flag (eq/ne)
        // applied to the result and zero is exactly the comparison COL asked for
        final var opZero = lcg.expression(ZERO_I32, lines, symbolTable);
        final var opResult = new TempOperand(symbolTable.nextTempName(), Bool.INSTANCE);
        lines.add(new BinaryOperation(opResult, ICMP, opCompare, opZero, new String[]{rcg.iFlag()}));
        return opResult;
    }
}
