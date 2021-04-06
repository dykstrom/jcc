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

import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Line;
import se.dykstrom.jcc.common.ast.AndExpression;
import se.dykstrom.jcc.common.code.Context;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.StorageLocation;

import java.util.List;

public class AndCodeGenerator extends AbstractExpressionCodeGeneratorComponent<AndExpression, TypeManager, AbstractCodeGenerator> {

    public AndCodeGenerator(Context context) { super(context); }

    @Override
    public List<Line> generate(AndExpression expression, StorageLocation leftLocation) {
        CodeContainer codeContainer = new CodeContainer();

        // Generate code for left sub expression, and store result in leftLocation
        codeGenerator.expression(expression.getLeft(), leftLocation);

        try (StorageLocation rightLocation = storageFactory.allocateNonVolatile()) {
            // Generate code for right sub expression, and store result in rightLocation
            codeGenerator.expression(expression.getRight(), rightLocation);
            // Generate code for and:ing sub expressions, and store result in leftLocation
            codeContainer.add(getComment(expression));
            leftLocation.andLocWithThis(rightLocation, codeContainer);
        }

        return codeContainer.lines();
    }
}
