/*
 * Copyright (C) 2026 Johan Dykstrom
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

package se.dykstrom.jcc.basic.code.llvm.statement;

import se.dykstrom.jcc.common.ast.ArrayAccessExpression;
import se.dykstrom.jcc.common.ast.AssignStatement;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.LlvmComment;
import se.dykstrom.jcc.llvm.LlvmUtils;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.AssignCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.LlvmStatementCodeGenerator;
import se.dykstrom.jcc.llvm.operation.StoreOperation;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.common.symbols.Scope.GLOBAL;

/**
 * Generates LLVM code for assignments. Assignment into an array element (LHS is an
 * {@link ArrayAccessExpression}) stores the value at the computed element address; all other
 * assignments are delegated to the shared scalar {@link AssignCodeGenerator}.
 */
public class BasicAssignCodeGenerator implements LlvmStatementCodeGenerator<AssignStatement> {

    private final LlvmCodeGenerator cg;
    private final AssignCodeGenerator scalarDelegate;

    public BasicAssignCodeGenerator(final LlvmCodeGenerator cg) {
        this.cg = requireNonNull(cg);
        this.scalarDelegate = new AssignCodeGenerator(cg, GLOBAL);
    }

    @Override
    public void toLlvm(final AssignStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        if (statement.getLhsExpression() instanceof ArrayAccessExpression arrayAccess) {
            lines.add(new LlvmComment(statement.toString()));
            final var opSource = cg.expression(statement.getRhsExpression(), lines, symbolTable);
            final var opAddress = LlvmUtils.arrayElementAddress(cg, arrayAccess, lines, symbolTable);
            lines.add(new StoreOperation(opSource, opAddress));
        } else {
            scalarDelegate.toLlvm(statement, lines, symbolTable);
        }
    }
}
