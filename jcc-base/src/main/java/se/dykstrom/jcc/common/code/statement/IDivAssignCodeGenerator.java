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

import se.dykstrom.jcc.common.ast.IDivAssignStatement;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.I64;

import java.util.List;

import static se.dykstrom.jcc.common.assembly.base.Register.RAX;
import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;

public class IDivAssignCodeGenerator extends AbstractStatementCodeGenerator<IDivAssignStatement, TypeManager, AbstractCodeGenerator> {

    public IDivAssignCodeGenerator(final AbstractCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(final IDivAssignStatement statement) {
        return withCodeContainer(cc -> {
            cc.add(getComment(statement));

            // Find type of identifier
            final var lhsType = types().getType(statement.lhsExpression());
            final var immediate = statement.rhsExpression().getValue();

            if (lhsType instanceof I64) {
                // Allocate RAX for identifier, because RAX is used by the idiv instruction
                final StorageLocation rax = storageFactory().get(RAX);
                // Allocate temporary storage for immediate value
                try (StorageLocation location = storageFactory().allocateNonVolatile(lhsType)) {
                    cc.addAll(codeGenerator.withAddressOfIdentifier(
                            statement.lhsExpression(),
                            (base, offset) -> withCodeContainer(it -> {
                                // Move immediate to temporary location, because we cannot use it directly
                                location.moveImmToThis(immediate, it);
                                rax.moveMemToThis(base + offset, it);
                                rax.idivThisWithLoc(location, it);
                                rax.moveThisToMem(base + offset, it);
                            })
                    ));
                }
            } else {
                throw new UnsupportedOperationException("IDIV is not supported on floating point values");
            }
        });
    }
}
