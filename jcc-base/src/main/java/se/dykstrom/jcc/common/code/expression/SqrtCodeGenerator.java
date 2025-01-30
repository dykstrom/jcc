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

import se.dykstrom.jcc.common.assembly.instruction.floating.SqrtFloat;
import se.dykstrom.jcc.common.ast.SqrtExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.compiler.AsmCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.FloatRegisterStorageLocation;
import se.dykstrom.jcc.common.storage.StorageLocation;

import java.util.List;

import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class SqrtCodeGenerator extends AbstractExpressionCodeGenerator<SqrtExpression, TypeManager, AsmCodeGenerator> {

    public SqrtCodeGenerator(final AsmCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(final SqrtExpression expression, final StorageLocation location) {
        return withCodeContainer(cc -> {
            cc.add(getComment(expression));

            final var arg = expression.getExpression();
            final var argType = types().getType(arg);
            if (location.stores(argType)) {
                cc.addAll(codeGenerator.expression(arg, location));
            } else {
                try (StorageLocation tmpLocation = codeGenerator.storageFactory().allocateNonVolatile(argType)) {
                    cc.addAll(codeGenerator.expression(arg, tmpLocation));
                    location.roundAndMoveLocToThis(tmpLocation, cc);
                }
            }
            // We "know" location is a floating point register
            final var register = ((FloatRegisterStorageLocation) location).getRegister();
            cc.add(new SqrtFloat(register, register));
        });
    }
}
