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

package se.dykstrom.jcc.common.code.expression;

import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.assembly.instruction.Jne;
import se.dykstrom.jcc.common.ast.LogicalOrExpression;
import se.dykstrom.jcc.common.code.Blank;
import se.dykstrom.jcc.common.code.CodeContainer;
import se.dykstrom.jcc.common.code.Label;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.StorageLocation;

import java.util.List;

import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class LogicalOrCodeGenerator extends AbstractExpressionCodeGenerator<LogicalOrExpression, TypeManager, AbstractCodeGenerator> {

    public LogicalOrCodeGenerator(final AbstractCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(final LogicalOrExpression expression, final StorageLocation leftLocation) {
        return CodeContainer.withCodeContainer(cc -> {
            // Generate unique label name
            final Label afterOrLabel = new Label(codeGenerator.uniquifyLabelName("after_or_"));

            // Generate code for left sub expression, and store result in leftLocation
            cc.addAll(codeGenerator.expression(expression.getLeft(), leftLocation));

            cc.add(new AssemblyComment("If the left expression (" + leftLocation + ") is true, short-circuit the OR expression"));
            leftLocation.compareThisWithImm("0", cc);
            cc.add(new Jne(afterOrLabel));

            try (final StorageLocation rightLocation = storageFactory().allocateNonVolatile()) {
                // Generate code for right sub expression, and store result in rightLocation
                cc.addAll(codeGenerator.expression(expression.getRight(), rightLocation));
                // Generate code for and:ing sub expressions, and store result in leftLocation
                cc.add(getComment(expression));
                leftLocation.orLocWithThis(rightLocation, cc);
            }

            // Add short-circuit label
            cc.add(afterOrLabel);
            cc.add(Blank.INSTANCE);
        });
    }
}
