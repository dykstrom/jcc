/*
 * Copyright (C) 2023 Johan Dykstrom
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

package se.dykstrom.jcc.col.code.statement;

import java.util.List;

import se.dykstrom.jcc.col.ast.FunCallStatement;
import se.dykstrom.jcc.col.compiler.ColCodeGenerator;
import se.dykstrom.jcc.common.code.expression.FunctionCallCodeGenerator;
import se.dykstrom.jcc.common.code.statement.AbstractStatementCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.intermediate.Line;

import static se.dykstrom.jcc.common.intermediate.CodeContainer.withCodeContainer;

public class FunCallCodeGenerator extends AbstractStatementCodeGenerator<FunCallStatement, TypeManager, ColCodeGenerator> {

    private final FunctionCallCodeGenerator fcCodeGenerator;

    public FunCallCodeGenerator(final ColCodeGenerator codeGenerator) {
        super(codeGenerator);
        fcCodeGenerator = new FunctionCallCodeGenerator(codeGenerator);
    }

    @Override
    public List<Line> generate(final FunCallStatement statement) {
        return withCodeContainer(cc -> cc.addAll(fcCodeGenerator.generate(statement.expression(), null)));
    }
}
