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

import se.dykstrom.jcc.common.ast.ModExpression;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.compiler.AsmCodeGenerator;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.I64;

import java.util.List;

import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.CF_FMOD_F64_F64;
import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class ModCodeGenerator extends AbstractBinaryExpressionCodeGenerator<ModExpression> {

    public ModCodeGenerator(final AsmCodeGenerator codeGenerator) { super(codeGenerator, StorageLocation::modThisWithLoc); }

    @Override
    public List<Line> generate(final ModExpression expression, final StorageLocation leftLocation) {
        final var lt = types().getType(expression.getLeft());
        final var rt = types().getType(expression.getRight());

        if (lt instanceof I64 && rt instanceof I64) {
            return super.generate(expression, leftLocation);
        } else {
            return withCodeContainer(cc -> {
                // The modulo operator for floats is implemented as a call to the libc function 'fmod'
                final var args = List.of(expression.getLeft(), expression.getRight());
                cc.addAll(codeGenerator.functionCall(CF_FMOD_F64_F64, getComment(expression), args, leftLocation));
            });
        }
    }
}
