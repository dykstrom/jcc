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

import se.dykstrom.jcc.basic.ast.expression.LboundExpression;
import se.dykstrom.jcc.basic.compiler.BasicCodeGenerator;
import se.dykstrom.jcc.llvm.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.llvm.code.Comment;
import se.dykstrom.jcc.llvm.code.expression.LlvmExpressionCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LiteralOperand;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;

import java.util.List;

/**
 * Generates code for the inlined 'lbound' function. The lower bound is the OPTION BASE value
 * (0 or 1), a compile-time constant, so no code is emitted.
 */
public record LboundCodeGenerator(BasicCodeGenerator cg) implements LlvmExpressionCodeGenerator<LboundExpression> {

    @Override
    public LlvmOperand toLlvm(final LboundExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        lines.add(new Comment(expression.toString()));
        return new LiteralOperand((long) cg.optionBase(), I64.INSTANCE);
    }
}
