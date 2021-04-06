/*
 * Copyright (C) 2021 Johan Dykstrom
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

package se.dykstrom.jcc.common.code;

import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Line;
import se.dykstrom.jcc.common.ast.ArrayDeclaration;
import se.dykstrom.jcc.common.ast.VariableDeclarationStatement;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.types.Arr;
import se.dykstrom.jcc.common.types.Identifier;

import java.util.List;

public class VariableDeclarationCodeGenerator extends AbstractCodeGeneratorComponent<VariableDeclarationStatement, TypeManager, AbstractCodeGenerator> {

    public VariableDeclarationCodeGenerator(Context context) { super(context); }

    @Override
    public List<Line> generate(VariableDeclarationStatement statement) {
        CodeContainer codeContainer = new CodeContainer();

        // For each declaration
        statement.getDeclarations().forEach(declaration -> {
            // Add variable to symbol table
            if (declaration.getType() instanceof Arr) {
                symbols.addArray(new Identifier(declaration.getName(), declaration.getType()), (ArrayDeclaration) declaration);
                // For $DYNAMIC arrays we also need to add initialization code here
            } else {
                symbols.addVariable(new Identifier(declaration.getName(), declaration.getType()));
            }
        });

        return codeContainer.lines();
    }
}
