/*
 * Copyright (C) 2022 Johan Dykstrom
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

import se.dykstrom.jcc.basic.ast.OptionBaseStatement;
import se.dykstrom.jcc.basic.compiler.BasicCodeGenerator;
import se.dykstrom.jcc.basic.compiler.BasicTypeManager;
import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.ast.IntegerLiteral;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.statement.AbstractStatementCodeGenerator;

import java.util.List;

import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_OPTION_BASE;
import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class OptionBaseCodeGenerator extends AbstractStatementCodeGenerator<OptionBaseStatement, BasicTypeManager, BasicCodeGenerator> {

    public OptionBaseCodeGenerator(final BasicCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(OptionBaseStatement statement) {
        return withCodeContainer(cc -> {
            cc.add(getComment(statement));
            final var functionComment = new AssemblyComment(FUN_OPTION_BASE.getName() + "()");
            final var baseExpression = new IntegerLiteral(statement.line(), statement.column(), statement.base());
            cc.addAll(codeGenerator.functionCall(FUN_OPTION_BASE, functionComment, singletonList(baseExpression)));
        });
    }
}
