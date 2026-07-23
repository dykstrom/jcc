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

import se.dykstrom.jcc.basic.ast.expression.ImpExpression;
import se.dykstrom.jcc.common.ast.NotExpression;
import se.dykstrom.jcc.common.ast.OrExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.LlvmExpressionCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;

import java.util.List;

public record ImpCodeGenerator(LlvmCodeGenerator codeGenerator) implements LlvmExpressionCodeGenerator<ImpExpression> {

    @Override
    public LlvmOperand toLlvm(final ImpExpression e, final List<Line> lines, final SymbolTable symbolTable) {
        return codeGenerator.expression(
                // a IMP b == NOT(a) OR b
                new OrExpression(new NotExpression(e.getLeft()), e.getRight()),
                lines,
                symbolTable
        );
    }
}
