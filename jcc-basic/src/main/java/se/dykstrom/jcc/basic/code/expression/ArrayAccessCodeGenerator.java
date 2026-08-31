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

package se.dykstrom.jcc.basic.code.expression;

import se.dykstrom.jcc.common.ast.ArrayAccessExpression;
import se.dykstrom.jcc.llvm.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Arr;
import se.dykstrom.jcc.llvm.code.Comment;
import se.dykstrom.jcc.llvm.LlvmUtils;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.LlvmExpressionCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.LoadOperation;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Generates LLVM code for reading an array element: the element address is obtained via
 * {@link LlvmUtils#arrayElementAddress} and the value is loaded.
 */
public record ArrayAccessCodeGenerator(LlvmCodeGenerator cg) implements LlvmExpressionCodeGenerator<ArrayAccessExpression> {

    public ArrayAccessCodeGenerator {
        requireNonNull(cg);
    }

    @Override
    public LlvmOperand toLlvm(final ArrayAccessExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        lines.add(new Comment(expression.toString()));
        final var opAddress = LlvmUtils.arrayElementAddress(cg, expression, lines, symbolTable);
        final var elementType = ((Arr) expression.getIdentifier().type()).getElementType();
        final var opResult = new TempOperand(symbolTable.nextTempName(), elementType);
        lines.add(new LoadOperation(opResult, opAddress));
        return opResult;
    }
}
