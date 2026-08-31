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

package se.dykstrom.jcc.llvm.code.statement;

import se.dykstrom.jcc.common.ast.ArrayAccessExpression;
import se.dykstrom.jcc.common.ast.AssignStatement;
import se.dykstrom.jcc.llvm.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.llvm.LlvmComment;
import se.dykstrom.jcc.llvm.LlvmUtils;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.common.symbols.Scope;
import se.dykstrom.jcc.llvm.operand.TempOperand;
import se.dykstrom.jcc.llvm.operation.StoreOperation;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Generates LLVM code for assignments. Assignment into an array element (LHS is an
 * {@link ArrayAccessExpression}) stores the value at the computed element address;
 * all other assignments store the value in the scalar variable, defining it first
 * if needed.
 */
public class AssignCodeGenerator implements LlvmStatementCodeGenerator<AssignStatement> {

    private final LlvmCodeGenerator cg;
    private final Scope scope;

    public AssignCodeGenerator(final LlvmCodeGenerator cg, final Scope scope) {
        this.cg = requireNonNull(cg);
        this.scope = requireNonNull(scope);
    }

    @Override
    public void toLlvm(final AssignStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        if (statement.getLhsExpression() instanceof ArrayAccessExpression arrayAccess) {
            lines.add(new LlvmComment(statement.toString()));
            // Compute the address before the RHS to evaluate the statement left-to-right
            final var opAddress = LlvmUtils.arrayElementAddress(cg, arrayAccess, lines, symbolTable);
            final var opSource = cg.expression(statement.getRhsExpression(), lines, symbolTable);
            lines.add(new StoreOperation(opSource, opAddress));
            return;
        }
        final var identifier = statement.getLhsExpression().getIdentifier();
        if (symbolTable.contains(identifier.name())) {
            toLlvm(statement, identifier, lines, symbolTable);
            return;
        }
        switch (scope) {
            case GLOBAL -> symbolTable.addGlobal(identifier, identifier.type().llvmDefaultValue());
            case LOCAL -> symbolTable.addVariable(identifier, identifier.type().llvmDefaultValue());
            case NONE -> throw new IllegalStateException(identifier.name() + " not found");
        }
        toLlvm(statement, identifier, lines, symbolTable);
    }

    private void toLlvm(final AssignStatement statement,
                        final Identifier identifier,
                        final List<Line> lines,
                        final SymbolTable symbolTable) {
        final var opSource = cg.expression(statement.getRhsExpression(), lines, symbolTable);
        final var opDestination = new TempOperand(symbolTable.mapName(identifier), identifier.type());
        // Store new value
        lines.add(new StoreOperation(opSource, opDestination));
    }
}
