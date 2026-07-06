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

package se.dykstrom.jcc.basic.code.llvm.expression;

import se.dykstrom.jcc.common.ast.BinaryExpression;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.llvm.LlvmComment;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.BinaryCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.LlvmExpressionCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.CallOperation;

import java.util.List;

import static se.dykstrom.jcc.basic.compiler.LibJccBasBuiltIns.JF_ADD_STR_STR;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.CF_FREE_I64;
import static se.dykstrom.jcc.llvm.LlvmUtils.allocatesTransientDynamicMemory;

/**
 * BASIC specific class that supports adding strings.
 */
public record BasicAddCodeGenerator(LlvmCodeGenerator lcg, BinaryCodeGenerator bcg)
        implements LlvmExpressionCodeGenerator<BinaryExpression> {

    @Override
    public LlvmOperand toLlvm(final BinaryExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        // Add strings using library function, and numbers using the default code generator
        return isString(expression.getLeft()) ?
                add(expression, lines, symbolTable) :
                bcg.toLlvm(expression, lines, symbolTable);
    }

    private boolean isString(final Expression expression) {
        return lcg.typeManager().getType(expression) instanceof Str;
    }

    private LlvmOperand add(final BinaryExpression e, final List<Line> lines, final SymbolTable symbolTable) {
        // Call add
        final var opLeft = lcg.expression(e.getLeft(), lines, symbolTable);
        final var opRight = lcg.expression(e.getRight(), lines, symbolTable);
        final var opResult = new TempOperand(symbolTable.nextTempName(), JF_ADD_STR_STR.getReturnType());
        lines.add(new CallOperation(opResult, JF_ADD_STR_STR, List.of(opLeft, opRight)));
        // Free temporary memory if needed
        if (allocatesTransientDynamicMemory(e.getLeft(), Str.INSTANCE)) {
            lines.add(new LlvmComment("Free dynamic memory in " + opLeft.toText()));
            final var opFreeResult = new TempOperand(symbolTable.nextTempName(), CF_FREE_I64.getReturnType());
            lines.add(new CallOperation(opFreeResult, CF_FREE_I64, List.of(opLeft)));
        }
        if (allocatesTransientDynamicMemory(e.getRight(), Str.INSTANCE)) {
            lines.add(new LlvmComment("Free dynamic memory in " + opRight.toText()));
            final var opFreeResult = new TempOperand(symbolTable.nextTempName(), CF_FREE_I64.getReturnType());
            lines.add(new CallOperation(opFreeResult, CF_FREE_I64, List.of(opRight)));
        }
        return opResult;
    }
}
