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

package se.dykstrom.jcc.common.code.statement;

import se.dykstrom.jcc.common.ast.MulAssignStatement;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.I64;

import java.util.List;

import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;

public class MulAssignCodeGenerator extends AbstractStatementCodeGenerator<MulAssignStatement, TypeManager, AbstractCodeGenerator> {

    public MulAssignCodeGenerator(final AbstractCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(final MulAssignStatement statement) {
        return withCodeContainer(cc -> {
            cc.add(getComment(statement));

            // Find type of identifier
            final var lhsType = types().getType(statement.lhsExpression());
            final var immediate = statement.rhsExpression().getValue();

            if (lhsType instanceof I64) {
                // Allocate temporary storage for identifier
                try (final StorageLocation location = storageFactory().allocateNonVolatile(lhsType)) {
                    // Multiply literal value with identifier
                    cc.addAll(codeGenerator.withAddressOfIdentifier(
                            statement.lhsExpression(),
                            (base, offset) -> withCodeContainer(it -> {
                                location.moveMemToThis(base + offset, it);
                                location.multiplyImmWithThis(immediate, it);
                                location.moveThisToMem(base + offset, it);
                            })
                    ));
                }
            } else {
                throw new UnsupportedOperationException("Operation *= is not supported for floating point values");
            }
        });
    }
}
