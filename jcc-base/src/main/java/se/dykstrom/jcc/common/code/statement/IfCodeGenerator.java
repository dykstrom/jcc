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

import se.dykstrom.jcc.common.code.Label;
import se.dykstrom.jcc.common.assembly.instruction.Je;
import se.dykstrom.jcc.common.assembly.instruction.Jmp;
import se.dykstrom.jcc.common.ast.IfStatement;
import se.dykstrom.jcc.common.code.Blank;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.StorageLocation;

import java.util.List;

import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class IfCodeGenerator extends AbstractStatementCodeGenerator<IfStatement, TypeManager, AbstractCodeGenerator> {

    public IfCodeGenerator(final AbstractCodeGenerator codeGenerator) {
        super(codeGenerator);
    }

    @Override
    public List<Line> generate(IfStatement statement) {
        // Generate unique label names
        Label afterThenLabel = new Label(codeGenerator.uniquifyLabelName("after_then_"));
        Label afterElseLabel = new Label(codeGenerator.uniquifyLabelName("after_else_"));

        try (StorageLocation location = storageFactory().allocateNonVolatile()) {
            // Generate code for the expression
            codeGenerator.addAll(codeGenerator.expression(statement.getExpression(), location));
            codeGenerator.add(Blank.INSTANCE);
            codeGenerator.add(getComment(statement));
            // If FALSE, jump to ELSE clause
            location.compareThisWithImm("0", codeGenerator); // FALSE
            codeGenerator.add(new Je(afterThenLabel));
        }

        // Generate code for THEN clause
        codeGenerator.add(Blank.INSTANCE);
        statement.getThenStatements().forEach(codeGenerator::statement);
        if (!statement.getElseStatements().isEmpty()) {
            // Only generate jump if there actually is an else clause
            codeGenerator.add(new Jmp(afterElseLabel));
        }
        codeGenerator.add(afterThenLabel);

        // Generate code for ELSE clause
        if (statement.getElseStatements() != null) {
            codeGenerator.add(Blank.INSTANCE);
            statement.getElseStatements().forEach(codeGenerator::statement);
            codeGenerator.add(afterElseLabel);
        }
        return List.of();
    }
}
