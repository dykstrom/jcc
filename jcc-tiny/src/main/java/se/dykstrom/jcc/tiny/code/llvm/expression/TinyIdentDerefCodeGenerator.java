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

package se.dykstrom.jcc.tiny.code.llvm.expression;

import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.code.expression.IdentDerefCodeGenerator;
import se.dykstrom.jcc.llvm.operand.LlvmOperand;

import java.util.List;

/**
 * Tiny extension of {@link IdentDerefCodeGenerator} that also adds unknown identifiers to the symbol table.
 */
public class TinyIdentDerefCodeGenerator extends IdentDerefCodeGenerator {

    @Override
    public LlvmOperand toLlvm(final IdentifierDerefExpression expression, final List<Line> lines, final SymbolTable symbolTable) {
        final var identifier = expression.getIdentifier();
        // If the identifier is undefined, add it to the symbol table now
        if (!symbolTable.contains(identifier.name())) {
            symbolTable.addVariable(identifier, "%" + identifier.name());
        }
        return super.toLlvm(expression, lines, symbolTable);
    }
}
