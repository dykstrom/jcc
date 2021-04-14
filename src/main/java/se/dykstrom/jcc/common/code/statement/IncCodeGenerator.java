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
import se.dykstrom.jcc.common.assembly.instruction.IncMem;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.IncStatement;
import se.dykstrom.jcc.common.code.Context;
import se.dykstrom.jcc.common.compiler.AbstractCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.types.I64;

import java.util.List;

import static se.dykstrom.jcc.common.assembly.base.CodeContainer.withCodeContainer;

public class IncCodeGenerator extends AbstractStatementCodeGeneratorComponent<IncStatement, TypeManager, AbstractCodeGenerator> {

    public IncCodeGenerator(Context context) { super(context); }

    @Override
    public List<Line> generate(IncStatement statement) {
        CodeContainer cc = new CodeContainer();

        Expression expression = statement.getLhsExpression();
        if (types.getType(expression) instanceof I64) {
            getLabel(statement).ifPresent(cc::add);
            cc.add(getComment(statement));
            cc.addAll(codeGenerator.withAddressOfIdentifier(
                    statement.getLhsExpression(),
                    (base, offset) -> withCodeContainer(it -> it.add(new IncMem(base + offset)))
            ));
        } else {
            throw new IllegalArgumentException("inc '" + expression + "' not supported");
        }

        return cc.lines();
    }
}
