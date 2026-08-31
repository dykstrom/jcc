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

import se.dykstrom.jcc.common.ast.ArrayDeclaration;
import se.dykstrom.jcc.common.ast.VariableDeclarationStatement;
import se.dykstrom.jcc.llvm.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Identifier;

import java.util.List;

public class VariableDeclarationCodeGenerator implements LlvmStatementCodeGenerator<VariableDeclarationStatement> {

    @Override
    public void toLlvm(final VariableDeclarationStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        statement.getDeclarations().forEach(declaration -> {
            final var identifier = new Identifier(declaration.name(), declaration.type());
            // Arrays are static module-level globals emitted by generateGlobals; register the
            // array (with its subscripts) so it can be found there, and skip the scalar path,
            // which would call the unsupported Arr.llvmDefaultValue().
            if (declaration instanceof ArrayDeclaration arrayDeclaration) {
                symbolTable.addGlobalArray(identifier, arrayDeclaration);
                return;
            }
            switch (statement.getScope()) {
                case GLOBAL -> symbolTable.addGlobal(identifier, declaration.type().llvmDefaultValue());
                case LOCAL -> symbolTable.addVariable(identifier, declaration.type().llvmDefaultValue());
                case NONE -> throw new IllegalArgumentException("scope " + statement.getScope() + " not supported");
            }
        });
    }
}
