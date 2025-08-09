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

import se.dykstrom.jcc.common.ast.ExitStatement;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;

import java.util.List;

import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.functions.LibcBuiltIns.CF_EXIT_I64;
import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class ExitCodeGenerator extends AbstractStatementCodeGenerator<ExitStatement, TypeManager, AbstractCodeGenerator> {

    public ExitCodeGenerator(final AbstractCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(ExitStatement statement) {
        return withCodeContainer(cc -> cc.addAll(codeGenerator.functionCall(CF_EXIT_I64, getComment(statement), singletonList(statement.getExpression()))));
    }
}
