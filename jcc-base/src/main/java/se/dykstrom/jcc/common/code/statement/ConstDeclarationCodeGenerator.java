/*
 * Copyright (C) 2023 Johan Dykstrom
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

package se.dykstrom.jcc.common.code.statement;

import se.dykstrom.jcc.common.ast.ConstDeclarationStatement;
import se.dykstrom.jcc.common.ast.DeclarationAssignment;
import se.dykstrom.jcc.common.ast.LiteralExpression;
import se.dykstrom.jcc.common.ast.StringLiteral;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.compiler.AsmCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.types.Identifier;

import java.util.List;

import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class ConstDeclarationCodeGenerator extends AbstractStatementCodeGenerator<ConstDeclarationStatement, TypeManager, AsmCodeGenerator> {

    public ConstDeclarationCodeGenerator(final AsmCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(final ConstDeclarationStatement statement) {
        return withCodeContainer(cc -> {
            // For each declaration
            statement.getDeclarations().forEach(declaration ->
                // Add constant to symbol table
                symbols().addConstant(new Identifier(declaration.name(), declaration.type()), getValue(declaration))
            );
            cc.add(getComment(statement));
        });
    }

    private static String getValue(final DeclarationAssignment declaration) {
        // We assume the semantics parser has reduced the initial expression to a literal
        final var expression = (LiteralExpression) declaration.expression();
        if (expression instanceof StringLiteral) {
            return "\"" + expression.getValue() + "\",0";
        } else {
            return expression.getValue();
        }
    }
}
