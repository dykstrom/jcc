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

package se.dykstrom.jcc.llvm.code.statement;

import se.dykstrom.jcc.common.ast.ConstDeclarationStatement;
import se.dykstrom.jcc.common.ast.LiteralExpression;
import se.dykstrom.jcc.llvm.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Identifier;

import java.util.List;

public class ConstDeclarationCodeGenerator implements LlvmStatementCodeGenerator<ConstDeclarationStatement> {

    @Override
    public void toLlvm(final ConstDeclarationStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        statement.getDeclarations().forEach(declaration -> {
            final var identifier = new Identifier(declaration.name(), declaration.type());
            final var expression = (LiteralExpression) declaration.expression();
            symbolTable.addConstant(identifier, expression.getValue());
        });
    }
}
