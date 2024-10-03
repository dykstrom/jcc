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

package se.dykstrom.jcc.assembunny.code.expression;

import se.dykstrom.jcc.assembunny.ast.RegisterExpression;
import se.dykstrom.jcc.assembunny.compiler.AssembunnyCodeGenerator;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.code.expression.AbstractExpressionCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.StorageLocation;

import java.util.List;

import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;
import static se.dykstrom.jcc.common.utils.AsmUtils.getComment;

/**
 * Generates code for evaluating an Assembunny register expression, that is, storing
 * the value of the register in the expression in a storage location.
 */
public class AssembunnyRegisterCodeGenerator extends AbstractExpressionCodeGenerator<RegisterExpression, TypeManager, AssembunnyCodeGenerator> {

    public AssembunnyRegisterCodeGenerator(final AssembunnyCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(RegisterExpression expression, StorageLocation location) {
        return withCodeContainer(cc -> {
            cc.add(getComment(expression));
            location.moveLocToThis(codeGenerator.getCpuRegister(expression.getRegister()), cc);
        });
    }
}
