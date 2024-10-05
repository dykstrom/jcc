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

import se.dykstrom.jcc.common.ast.BinaryExpression;
import se.dykstrom.jcc.common.code.CodeContainer;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.compiler.AsmCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.Type;

import java.util.List;

import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public abstract class AbstractBinaryExpressionCodeGenerator<E extends BinaryExpression>
        extends AbstractExpressionCodeGenerator<E, TypeManager, AsmCodeGenerator> {

    private final BinaryExpressionCodeGeneratorFunction codeGeneratorFunction;

    protected interface BinaryExpressionCodeGeneratorFunction {
        void generate(StorageLocation left, StorageLocation right, CodeContainer cc);
    }

    protected AbstractBinaryExpressionCodeGenerator(final AsmCodeGenerator codeGenerator,
                                                    final BinaryExpressionCodeGeneratorFunction codeGeneratorFunction) {
        super(codeGenerator);
        this.codeGeneratorFunction = codeGeneratorFunction;
    }

    @Override
    public List<Line> generate(E expression, StorageLocation leftLocation) {
        CodeContainer cc = new CodeContainer();

        // Generate code for left sub expression, and store result in leftLocation
        cc.addAll(codeGenerator.expression(expression.getLeft(), leftLocation));

        // Find type of right sub expression
        Type type = types().getType(expression.getRight());

        try (StorageLocation rightLocation = storageFactory().allocateNonVolatile(type)) {
            // Generate code for right sub expression, and store result in rightLocation
            cc.addAll(codeGenerator.expression(expression.getRight(), rightLocation));

            // Generate code using sub expressions
            cc.add(getComment(expression));
            codeGeneratorFunction.generate(leftLocation, rightLocation, cc);
        }

        return cc.lines();
    }
}
