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

import se.dykstrom.jcc.common.ast.CastToI32Expression;
import se.dykstrom.jcc.common.assembly.base.Register32;
import se.dykstrom.jcc.common.assembly.instruction.MoveWithSignExtend;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.compiler.AsmCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.RegisterStorageLocation;
import se.dykstrom.jcc.common.storage.StorageLocation;

import java.util.List;

import static se.dykstrom.jcc.common.assembly.base.Register.RAX;
import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class CastToI32CodeGenerator extends AbstractExpressionCodeGenerator<CastToI32Expression, TypeManager, AsmCodeGenerator> {

    public CastToI32CodeGenerator(final AsmCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(final CastToI32Expression expression, final StorageLocation location) {
        return withCodeContainer(cc -> {
            // We assume that location can store expression, because we know expression is of integer type
            cc.add(getComment(expression));
            cc.addAll(codeGenerator.expression(expression.getExpression(), location));

            final StorageLocation rax = storageFactory().get(RAX);
            final Register32 eax = RAX.asLowRegister32();

            // Move value in location to RAX
            rax.moveLocToThis(location, cc);
            if (location instanceof RegisterStorageLocation rsl) {
                final var register = rsl.getRegister();
                // Move value in EAX back to location
                cc.add(new MoveWithSignExtend(eax, register));
            } else {
                throw new IllegalArgumentException("not supported for location of type: " + location.getClass().getSimpleName());
            }
        });
    }
}
