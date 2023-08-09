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

package se.dykstrom.jcc.basic.code.statement;

import se.dykstrom.jcc.basic.ast.OnGotoStatement;
import se.dykstrom.jcc.basic.compiler.BasicCodeGenerator;
import se.dykstrom.jcc.basic.compiler.BasicTypeManager;
import se.dykstrom.jcc.common.intermediate.Blank;
import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.intermediate.Line;
import se.dykstrom.jcc.common.assembly.instruction.Je;
import se.dykstrom.jcc.common.code.Context;
import se.dykstrom.jcc.common.code.statement.AbstractStatementCodeGeneratorComponent;
import se.dykstrom.jcc.common.storage.StorageLocation;

import java.util.List;

import static se.dykstrom.jcc.common.intermediate.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.compiler.AbstractCodeGenerator.lineToLabel;

public class OnGotoCodeGenerator extends AbstractStatementCodeGeneratorComponent<OnGotoStatement, BasicTypeManager, BasicCodeGenerator> {

    public OnGotoCodeGenerator(Context context) {
        super(context);
    }

    @Override
    public List<Line> generate(OnGotoStatement statement) {
        return withCodeContainer(cc -> {
            // Allocate a storage location for the on-goto expression
            try (StorageLocation location = storageFactory.allocateNonVolatile()) {
                cc.add(new AssemblyComment("Evaluate ON-GOTO expression"));

                // Generate code for the expression
                cc.addAll(codeGenerator.expression(statement.getExpression(), location));
                cc.add(Blank.INSTANCE);
                cc.add(getComment(statement));

                for (int index = 0; index < statement.getJumpLabels().size(); index++) {
                    location.compareThisWithImm(Integer.toString(index + 1), cc);
                    Label jumpLabel = lineToLabel(statement.getJumpLabels().get(index));
                    cc.add(new Je(jumpLabel));
                }
            }
        });
    }
}
