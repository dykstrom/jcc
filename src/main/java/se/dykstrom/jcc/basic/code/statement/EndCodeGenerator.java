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

import se.dykstrom.jcc.basic.ast.EndStatement;
import se.dykstrom.jcc.basic.compiler.BasicCodeGenerator;
import se.dykstrom.jcc.basic.compiler.BasicTypeManager;
import se.dykstrom.jcc.common.assembly.base.Line;
import se.dykstrom.jcc.common.code.Context;
import se.dykstrom.jcc.common.code.statement.AbstractStatementCodeGeneratorComponent;

import java.util.List;

import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.common.assembly.base.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_EXIT;

public class EndCodeGenerator extends AbstractStatementCodeGeneratorComponent<EndStatement, BasicTypeManager, BasicCodeGenerator> {

    public EndCodeGenerator(Context context) {
        super(context);
    }

    @Override
    public List<Line> generate(EndStatement statement) {
        return withCodeContainer(cc -> cc.addAll(codeGenerator.functionCall(FUN_EXIT, getComment(statement), singletonList(statement.getExpression()))));
    }
}
