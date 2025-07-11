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

import se.dykstrom.jcc.basic.ast.RandomizeStatement;
import se.dykstrom.jcc.basic.compiler.BasicCodeGenerator;
import se.dykstrom.jcc.basic.compiler.BasicTypeManager;
import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.code.Blank;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.statement.AbstractStatementCodeGenerator;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static se.dykstrom.jcc.basic.functions.LibJccBasBuiltIns.FUN_RANDOMIZE;
import static se.dykstrom.jcc.basic.functions.LibJccBasBuiltIns.FUN_VAL;
import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_GETLINE;
import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

public class RandomizeCodeGenerator extends AbstractStatementCodeGenerator<RandomizeStatement, BasicTypeManager, BasicCodeGenerator> {

    public RandomizeCodeGenerator(final BasicCodeGenerator codeGenerator) {
        super(codeGenerator);
    }

    @Override
    public List<Line> generate(RandomizeStatement statement) {
        return withCodeContainer(cc -> {
            cc.add(getComment(statement));

            Expression expression = statement.getExpression();
            if (expression == null) {
                // Print prompt
                cc.addAll(codeGenerator.printPrompt(statement, "Random Number Seed (-32768 to 32767)? "));
                cc.add(Blank.INSTANCE);
                // Read user input
                expression = new FunctionCallExpression(statement.line(), statement.column(), FUN_GETLINE.getIdentifier(), emptyList());
                expression = new FunctionCallExpression(statement.line(), statement.column(), FUN_VAL.getIdentifier(), singletonList(expression));
            }
            // Call randomize
            cc.addAll(codeGenerator.functionCall(FUN_RANDOMIZE, new AssemblyComment(FUN_RANDOMIZE.getName() + "(" + expression + ")"), singletonList(expression)));
        });
    }
}
