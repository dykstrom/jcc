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

import se.dykstrom.jcc.common.assembly.base.CodeContainer;
import se.dykstrom.jcc.common.assembly.base.Line;
import se.dykstrom.jcc.common.assembly.instruction.DecMem;
import se.dykstrom.jcc.common.ast.DecStatement;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.code.Context;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.types.I64;

import java.util.List;

import static se.dykstrom.jcc.common.assembly.base.CodeContainer.withCodeContainer;

public class DecCodeGenerator extends AbstractStatementCodeGeneratorComponent<DecStatement, TypeManager, AbstractCodeGenerator> {

    public DecCodeGenerator(Context context) { super(context); }

    @Override
    public List<Line> generate(DecStatement statement) {
        CodeContainer cc = new CodeContainer();

        Expression expression = statement.getLhsExpression();
        if (types.getType(expression) instanceof I64) {
            cc.add(getComment(statement));
            cc.addAll(codeGenerator.withAddressOfIdentifier(
                    statement.getLhsExpression(),
                    (base, offset) -> withCodeContainer(it -> it.add(new DecMem(base + offset)))
            ));
        } else {
            throw new IllegalArgumentException("dec '" + expression + "' not supported");
        }

        return cc.lines();
    }
}
