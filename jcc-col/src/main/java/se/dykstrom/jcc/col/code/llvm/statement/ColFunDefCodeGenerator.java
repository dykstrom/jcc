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

package se.dykstrom.jcc.col.code.llvm.statement;

import se.dykstrom.jcc.col.ast.expression.BecomeExpression;
import se.dykstrom.jcc.col.compiler.ColLlvmFunctions;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement;
import se.dykstrom.jcc.common.ast.IfExpression;
import se.dykstrom.jcc.common.code.FixedLabel;
import se.dykstrom.jcc.common.code.Label;
import se.dykstrom.jcc.common.code.Line;
import se.dykstrom.jcc.common.symbols.SymbolTable;
import se.dykstrom.jcc.llvm.LlvmComment;
import se.dykstrom.jcc.llvm.code.GcCodeGenerator;
import se.dykstrom.jcc.llvm.code.LlvmCodeGenerator;
import se.dykstrom.jcc.llvm.code.expression.FunctionCallCodeGenerator;
import se.dykstrom.jcc.llvm.code.statement.FunDefCodeGenerator;
import se.dykstrom.jcc.llvm.operation.BranchOperation;
import se.dykstrom.jcc.llvm.operation.ReturnOperation;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Generates COL function definitions. When a function body tail-calls another function with
 * {@code become}, the body must be generated in <em>tail context</em>: a {@code musttail} call
 * has to immediately precede the {@code ret} of its value, which the ordinary phi-merging
 * if-expression code generator cannot express. So a body containing a tail {@code become} is
 * generated here so that every tail leaf terminates its own block with a {@code ret} and tail
 * if-expressions branch without a merge block; a body with no {@code become} falls back to the
 * shared (phi-merging) code path unchanged.
 */
public class ColFunDefCodeGenerator extends FunDefCodeGenerator {

    private final FunctionCallCodeGenerator functionCallCodeGenerator;
    private final GcCodeGenerator gc;

    public ColFunDefCodeGenerator(final LlvmCodeGenerator codeGenerator, final GcCodeGenerator gc) {
        super(codeGenerator, gc);
        this.gc = requireNonNull(gc);
        this.functionCallCodeGenerator = new FunctionCallCodeGenerator(codeGenerator, new ColLlvmFunctions(), gc);
    }

    @Override
    protected List<Line> generateStatementLines(final FunctionDefinitionStatement statement,
                                                final SymbolTable symbolTable) {
        final var body = statement.expression();
        if (body != null && containsTailBecome(body)) {
            final var lines = new ArrayList<Line>();
            generateTail(body, lines, symbolTable);
            return lines;
        }
        return super.generateStatementLines(statement, symbolTable);
    }

    /**
     * Generates code for an expression in tail position. Each leaf terminates its own basic block
     * with a {@code ret}; a tail if-expression branches to self-terminating then/else blocks with
     * no merge block (so a both-{@code become} if produces no merge block at all).
     */
    private void generateTail(final Expression expression, final List<Line> lines, final SymbolTable symbolTable) {
        switch (expression) {
            case BecomeExpression become -> {
                final var opResult = functionCallCodeGenerator.toLlvmTailCall(become.functionCall(), lines, symbolTable);
                lines.add(new ReturnOperation(opResult));
            }
            case IfExpression ie -> {
                lines.add(new LlvmComment(ie.toString()));
                final Label thenLabel = new FixedLabel(symbolTable.nextLabelName());
                final Label elseLabel = new FixedLabel(symbolTable.nextLabelName());
                final var opCond = codeGenerator.expression(ie.ifExpr(), lines, symbolTable);
                lines.add(new BranchOperation(opCond, thenLabel, elseLabel));
                lines.add(thenLabel);
                generateTail(ie.thenExpr(), lines, symbolTable);
                lines.add(elseLabel);
                generateTail(ie.elseExpr(), lines, symbolTable);
            }
            default -> {
                // This leaf bypasses ReturnCodeGenerator, so it must close the GC frame itself -
                // after the value is evaluated, before the ret, as the ordinary return path does
                final var opResult = codeGenerator.expression(expression, lines, symbolTable);
                lines.addAll(gc.exitFunction());
                lines.add(new ReturnOperation(opResult));
            }
        }
    }

    /**
     * Returns whether the tail tree of the given expression contains a become expression. Tail
     * position is the expression itself and the then/else branches of a tail if-expression; this
     * does not descend into operands, arguments, or conditions. In a program that passed semantic
     * analysis, every tail become is a bare node (rule 3 forbids the widening cast).
     */
    private static boolean containsTailBecome(final Expression expression) {
        return switch (expression) {
            case BecomeExpression ignored -> true;
            case IfExpression ie -> containsTailBecome(ie.thenExpr()) || containsTailBecome(ie.elseExpr());
            default -> false;
        };
    }
}
