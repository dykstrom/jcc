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

import se.dykstrom.jcc.basic.ast.OnGosubStatement;
import se.dykstrom.jcc.basic.compiler.BasicCodeGenerator;
import se.dykstrom.jcc.basic.compiler.BasicTypeManager;
import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.assembly.base.Label;
import se.dykstrom.jcc.common.assembly.instruction.Je;
import se.dykstrom.jcc.common.assembly.instruction.Jmp;
import se.dykstrom.jcc.common.code.statement.AbstractStatementCodeGenerator;
import se.dykstrom.jcc.common.intermediate.Blank;
import se.dykstrom.jcc.common.intermediate.Line;
import se.dykstrom.jcc.common.storage.StorageLocation;

import java.util.ArrayList;
import java.util.List;

import static se.dykstrom.jcc.common.intermediate.CodeContainer.withCodeContainer;

public class OnGosubCodeGenerator extends AbstractStatementCodeGenerator<OnGosubStatement, BasicTypeManager, BasicCodeGenerator> {

    public OnGosubCodeGenerator(final BasicCodeGenerator codeGenerator) {
        super(codeGenerator);
    }

    @Override
    public List<Line> generate(OnGosubStatement statement) {
        return withCodeContainer(cc -> {
            // Allocate a storage location for the on-gosub expression
            try (StorageLocation location = storageFactory().allocateNonVolatile()) {
                cc.add(new AssemblyComment("Evaluate ON-GOSUB expression"));

                // Generate code for the expression
                cc.addAll(codeGenerator.expression(statement.getExpression(), location));
                cc.add(Blank.INSTANCE);
                cc.add(getComment(statement));

                List<Label> indexLabels = new ArrayList<>();

                // Generate code for comparing with indices
                for (int index = 0; index < statement.getJumpLabels().size(); index++) {
                    // Generate a unique label name for this index
                    Label indexLabel = new Label(codeGenerator.uniqifyLabelName("_on_gosub_index_"));
                    indexLabels.add(indexLabel);

                    // Compare with index and jump to index label
                    location.compareThisWithImm(Integer.toString(index + 1), cc);
                    cc.add(new Je(indexLabel));
                }

                // Generate a unique label name for the label that marks the end of the on-gosub statement
                Label endLabel = new Label(codeGenerator.uniqifyLabelName("_on_gosub_end_"));
                cc.add(new Jmp(endLabel));

                // Generate code for calling subroutines
                for (int index = 0; index < statement.getJumpLabels().size(); index++) {
                    cc.add(indexLabels.get(index));
                    cc.addAll(codeGenerator.callGosubLabel(statement.getJumpLabels().get(index)));
                    cc.add(new Jmp(endLabel));
                }
                cc.add(endLabel);
            }
        });
    }
}
