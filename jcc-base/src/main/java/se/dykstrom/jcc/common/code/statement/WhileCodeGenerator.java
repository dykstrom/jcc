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
import se.dykstrom.jcc.common.ast.WhileStatement;
import se.dykstrom.jcc.common.code.Blank;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.StorageLocation;

import java.util.List;

import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class WhileCodeGenerator extends AbstractStatementCodeGenerator<WhileStatement, TypeManager, AbstractCodeGenerator> {

    public WhileCodeGenerator(final AbstractCodeGenerator codeGenerator) {
        super(codeGenerator);
    }

    @Override
    public List<Line> generate(WhileStatement statement) {
        // Generate unique label names
        Label beforeWhileLabel = new Label(codeGenerator.uniquifyLabelName("before_while_"));
        Label afterWhileLabel = new Label(codeGenerator.uniquifyLabelName("after_while_"));

        // Add a label before the WHILE test
        codeGenerator.add(beforeWhileLabel);

        try (StorageLocation location = storageFactory().allocateNonVolatile()) {
            // Generate code for the expression
            codeGenerator.addAll(codeGenerator.expression(statement.getExpression(), location));
            codeGenerator.add(Blank.INSTANCE);
            codeGenerator.add(getComment(statement));
            // If FALSE, jump to after WHILE clause
            location.compareThisWithImm("0", codeGenerator); // FALSE
            codeGenerator.add(new Je(afterWhileLabel));
        }

        // Generate code for WHILE clause
        codeGenerator.add(Blank.INSTANCE);
        statement.getStatements().forEach(codeGenerator::statement);
        // Jump back to perform the test again
        codeGenerator.add(new Jmp(beforeWhileLabel));
        // Add a label after the WHILE clause
        codeGenerator.add(afterWhileLabel);
        return List.of();
    }
}
