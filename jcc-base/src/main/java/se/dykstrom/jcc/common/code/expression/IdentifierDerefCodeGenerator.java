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

package se.dykstrom.jcc.common.code.expression;

import se.dykstrom.jcc.common.code.Label;
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.compiler.AsmCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.functions.AssemblyFunction;
import se.dykstrom.jcc.common.functions.LibraryFunction;
import se.dykstrom.jcc.common.functions.UserDefinedFunction;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.Fun;

import java.util.List;

import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class IdentifierDerefCodeGenerator extends AbstractExpressionCodeGenerator<IdentifierDerefExpression, TypeManager, AsmCodeGenerator> {

    public IdentifierDerefCodeGenerator(final AsmCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(final IdentifierDerefExpression expression, final StorageLocation location) {
        return withCodeContainer(cc -> {
            cc.add(getComment(expression));
            final var name = expression.getIdentifier().name();
            if (symbols().contains(name)) {
                final var identifier = symbols().getIdentifier(name);
                // Store the identifier contents (not its address)
                location.moveMemToThis(identifier.getMappedName(), cc);
            } else if (symbols().containsFunction(name)) {
                final var functionType = (Fun) expression.getIdentifier().type();
                final var function = symbols().getFunction(name, functionType.getArgTypes());
                final var functionLabel = new Label(function.getMappedName());

                // Store the address of the function
                if (function instanceof AssemblyFunction) {
                    location.moveImmToThis(functionLabel.getMappedName(), cc);
                } else if (function instanceof UserDefinedFunction) {
                    location.moveImmToThis(functionLabel.getMappedName(), cc);
                } else if (function instanceof LibraryFunction) {
                    location.moveMemToThis(function.getMappedName(), cc);
                }
            }
        });
    }
}
