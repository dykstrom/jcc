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

package se.dykstrom.jcc.common.code.statement;

import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Line;
import se.dykstrom.jcc.common.ast.SubAssignStatement;
import se.dykstrom.jcc.common.code.Context;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.StorageLocation;
import se.dykstrom.jcc.common.types.Type;

import java.util.List;

public class SubAssignCodeGenerator extends AbstractStatementCodeGeneratorComponent<SubAssignStatement, TypeManager, AbstractCodeGenerator> {

    public SubAssignCodeGenerator(Context context) { super(context); }

    @Override
    public List<Line> generate(SubAssignStatement statement) {
        CodeContainer codeContainer = new CodeContainer();

        getLabel(statement).ifPresent(codeContainer::add);
        codeContainer.add(getComment(statement));

        // Find type of identifier
        Type lhsType = types.getType(statement.getLhsExpression());

        // Allocate temporary storage for identifier
        try (StorageLocation location = storageFactory.allocateNonVolatile(lhsType)) {
            // Subtract literal value from identifier
            String value = statement.getRhsExpression().getValue();
            codeGenerator.withAddressOfIdentifier(statement.getLhsExpression(), (base, offset) -> location.subtractImmFromMem(value, base + offset, codeContainer));
        }

        return codeContainer.lines();
    }
}
