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

import se.dykstrom.jcc.common.ast.LabelledStatement;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;

import java.util.List;

import static se.dykstrom.jcc.common.compiler.AbstractCodeGenerator.lineToLabel;

public class LabelledCodeGenerator extends AbstractStatementCodeGenerator<LabelledStatement, TypeManager, AbstractCodeGenerator> {

    public LabelledCodeGenerator(final AbstractCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(LabelledStatement statement) {
        // As long as CodeGenerator#statement(Statement) does not return the generated lines
        // we must call CodeGenerator#add(Line) to add new lines. Otherwise, the lines will
        // end up in the wrong order.
        codeGenerator.add(lineToLabel(statement.label()));
        codeGenerator.statement(statement.statement());
        return List.of();
    }
}
