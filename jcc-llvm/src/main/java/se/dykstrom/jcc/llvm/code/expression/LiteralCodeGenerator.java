/*
 * Copyright (C) 2024 Johan Dykstrom
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

import se.dykstrom.jcc.common.ast.BooleanLiteral;
import se.dykstrom.jcc.common.ast.LiteralExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.operand.LiteralOperand;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;

import java.util.List;

public class LiteralCodeGenerator implements LlvmExpressionCodeGenerator<LiteralExpression> {

    @Override
    public LlvmOperand toLlvm(final LiteralExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        final String value;
        if (BooleanLiteral.TRUE.equals(expression)) {
            // LLVM represents boolean true as 1, while the direct assembly generation uses -1
            // Returning 1 here is a workaround to be used while we support both backends
            value = "1";
        } else {
            value = expression.getValue();
        }
        return new LiteralOperand(value, expression.getType());
    }
}
