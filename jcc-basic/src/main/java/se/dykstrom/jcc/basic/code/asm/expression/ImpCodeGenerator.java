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

package se.dykstrom.jcc.basic.code.asm.expression;

import se.dykstrom.jcc.basic.ast.expression.ImpExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.expression.AbstractExpressionCodeGenerator;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.StorageLocation;

import java.util.List;

import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class ImpCodeGenerator extends AbstractExpressionCodeGenerator<ImpExpression, TypeManager, AbstractCodeGenerator> {

    public ImpCodeGenerator(final AbstractCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(ImpExpression expression, StorageLocation leftLocation) {
        return withCodeContainer(cc -> {
            // Generate code for left sub expression, and store result in leftLocation
            cc.addAll(codeGenerator.expression(expression.getLeft(), leftLocation));

            try (StorageLocation rightLocation = storageFactory().allocateNonVolatile()) {
                // Generate code for right sub expression, and store result in rightLocation
                cc.addAll(codeGenerator.expression(expression.getRight(), rightLocation));
                // a IMP b == NOT(a) OR b
                cc.add(getComment(expression));
                leftLocation.notThis(cc);
                leftLocation.orLocWithThis(rightLocation, cc);
            }
        });
    }
}
