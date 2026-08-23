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
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.llvm.code.GcCodeGenerator;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.BinaryCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.LlvmExpressionCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.CallOperation;

import java.util.List;

import static se.dykstrom.jcc.col.compiler.LibJccColBuiltIns.JF_CONCAT_STR_STR;

/**
 * COL specific class that supports concatenating strings. Numbers are added by the default
 * code generator; two strings are concatenated by a libjcccol call whose freshly allocated
 * result is handed to the garbage collector.
 */
public record ColAddCodeGenerator(LlvmCodeGenerator lcg, BinaryCodeGenerator bcg, GcCodeGenerator gc)
        implements LlvmExpressionCodeGenerator<BinaryExpression> {

    @Override
    public LlvmOperand toLlvm(final BinaryExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        return isString(expression.getLeft()) ?
                concat(expression, lines, symbolTable) :
                bcg.toLlvm(expression, lines, symbolTable);
    }

    private boolean isString(final Expression expression) {
        return lcg.typeManager().getType(expression) instanceof Str;
    }

    private LlvmOperand concat(final BinaryExpression e, final List<Line> lines, final SymbolTable symbolTable) {
        // Both operands are already registered and rooted by their own code generators if they
        // allocated, so they stay reachable across this call - nothing is freed here.
        final var opLeft = lcg.expression(e.getLeft(), lines, symbolTable);
        final var opRight = lcg.expression(e.getRight(), lines, symbolTable);
        final var opResult = new TempOperand(symbolTable.nextTempName(), JF_CONCAT_STR_STR.getReturnType());
        lines.add(new CallOperation(opResult, JF_CONCAT_STR_STR, List.of(opLeft, opRight)));
        // col_concat_str_str returns a freshly malloc'd string; hand it to the collector, which
        // also stores it into a rooted slot so it survives the next registration.
        return gc.registerResult(opResult, lines, symbolTable);
    }
}
