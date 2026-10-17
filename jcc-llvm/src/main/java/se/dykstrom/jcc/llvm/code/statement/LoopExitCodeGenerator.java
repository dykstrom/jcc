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

package se.dykstrom.jcc.llvm.code.statement;

import se.dykstrom.jcc.common.ast.LoopExitStatement;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.code.Comment;
import se.dykstrom.jcc.llvm.code.Line;
import se.dykstrom.jcc.llvm.code.LoopStack;
import se.dykstrom.jcc.llvm.operation.BranchOperation;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Generates LLVM code for loop exit statements, by branching to the label after the innermost
 * enclosing loop of the given kind. Each language must check that such a loop exists.
 */
public record LoopExitCodeGenerator(LoopStack loopStack) implements LlvmStatementCodeGenerator<LoopExitStatement> {

    public LoopExitCodeGenerator {
        requireNonNull(loopStack);
    }

    @Override
    public void toLlvm(final LoopExitStatement statement, final List<Line> lines, final SymbolTable symbolTable) {
        final var exitLabel = loopStack.innermost(statement.getLoop())
                .orElseThrow(() -> new IllegalStateException(statement + " outside " + statement.getLoop().getSimpleName()));
        lines.add(new Comment(statement.toString()));
        lines.add(new BranchOperation(exitLabel));
    }
}
