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

package se.dykstrom.jcc.basic.code.statement;

import se.dykstrom.jcc.basic.ast.SleepStatement;
import se.dykstrom.jcc.basic.compiler.BasicCodeGenerator;
import se.dykstrom.jcc.basic.compiler.BasicTypeManager;
import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FloatLiteral;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.statement.AbstractStatementCodeGenerator;

import java.util.List;

import static se.dykstrom.jcc.basic.functions.LibJccBasBuiltIns.FUN_SLEEP;
import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class SleepCodeGenerator extends AbstractStatementCodeGenerator<SleepStatement, BasicTypeManager, BasicCodeGenerator> {

    public SleepCodeGenerator(final BasicCodeGenerator codeGenerator) {
        super(codeGenerator);
    }

    @Override
    public List<Line> generate(SleepStatement statement) {
        return withCodeContainer(cc -> {
            cc.add(getComment(statement));

            Expression expression = statement.getExpression();
            if (expression == null) {
                expression = new FloatLiteral(statement.line(), statement.column(), 0.0);
            }
            // Call sleep function
            cc.addAll(codeGenerator.functionCall(
                    FUN_SLEEP,
                    new AssemblyComment(FUN_SLEEP.getMappedName() + "(" + expression + ")"),
                    List.of(expression)
            ));
        });
    }
}
