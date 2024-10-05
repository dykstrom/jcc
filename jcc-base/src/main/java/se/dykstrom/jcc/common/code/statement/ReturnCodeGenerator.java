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

import se.dykstrom.jcc.common.assembly.base.AssemblyComment;
import se.dykstrom.jcc.common.assembly.instruction.Ret;
import se.dykstrom.jcc.common.ast.ReturnStatement;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.compiler.AsmCodeGenerator;
import se.dykstrom.jcc.common.compiler.TypeManager;
import se.dykstrom.jcc.common.storage.StorageLocation;

import java.util.List;

import static se.dykstrom.jcc.common.assembly.base.Register.RAX;
import static se.dykstrom.jcc.common.code.CodeContainer.withCodeContainer;

public class ReturnCodeGenerator extends AbstractStatementCodeGenerator<ReturnStatement, TypeManager, AsmCodeGenerator> {

    public ReturnCodeGenerator(final AsmCodeGenerator codeGenerator) { super(codeGenerator); }

    @Override
    public List<Line> generate(final ReturnStatement statement) {
        return withCodeContainer(cc -> {
            if (statement.getExpression() != null) {
                try (StorageLocation location = storageFactory().allocateNonVolatile()) {
                    // Generate code for the expression
                    cc.addAll(codeGenerator.expression(statement.getExpression(), location));
                    // Move result to RAX
                    cc.add(new AssemblyComment("Move return value (" + location + ") to rax"));
                    codeGenerator.storageFactory().get(RAX).moveLocToThis(location, cc);
                }
            }
            cc.add(new Ret());
        });
    }
}
