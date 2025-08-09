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

package se.dykstrom.jcc.llvm.code.expression;

import se.dykstrom.jcc.common.ast.LiteralExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.llvm.operand.LiteralOperand;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;

import java.util.List;

public class StringLiteralCodeGenerator implements LlvmExpressionCodeGenerator<LiteralExpression> {

    /**
     * Indexing all static strings in the code, helping to create a unique name for each.
     */
    private int stringIndex = 0;

    @Override
    public LlvmOperand toLlvm(final LiteralExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        final var value = expression.getValue() + "\0";
        // Try to find an existing string constant with this value
        final var optionalIdentifier = symbolTable.getConstantByTypeAndValue(Str.INSTANCE, value);
        // If there was no string constant with this exact value before, create one
        final var identifier = optionalIdentifier.orElseGet(
                () -> symbolTable.addConstant(new Identifier(getUniqueStringName(), Str.INSTANCE), value)
        );
        // The value of the expression is the name of the global string constant
        return new LiteralOperand(identifier.name(), expression.getType());
    }

    /**
     * Returns a unique string constant name to use in the symbol table.
     */
    private String getUniqueStringName() {
        return "@.str." + stringIndex++;
    }
}
