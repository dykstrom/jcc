/*
 * Copyright (C) 2019 Johan Dykstrom
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

package se.dykstrom.jcc.basic.optimization;

import se.dykstrom.jcc.basic.ast.statement.PrintStatement;
import se.dykstrom.jcc.basic.ast.statement.RandomizeStatement;
import se.dykstrom.jcc.basic.ast.statement.SleepStatement;
import se.dykstrom.jcc.basic.type.BasicTypeManager;
import se.dykstrom.jcc.common.ast.AstProgram;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.NotEqualExpression;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.optimization.AstExpressionOptimizer;
import se.dykstrom.jcc.common.optimization.AstOptimizer;
import se.dykstrom.jcc.common.optimization.DefaultAstExpressionOptimizer;
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer;
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer.StatementComponent;
import se.dykstrom.jcc.common.symbols.SymbolTable;

import java.util.List;

import static se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO;

/**
 * The BASIC AST optimizer performs BASIC specific optimizations on the AST. It is a
 * {@link DefaultAstOptimizer} with BASIC's condition rewrite, and with optimizers for the
 * BASIC statements PRINT, RANDOMIZE and SLEEP.
 *
 * @author Johan Dykstrom
 */
public class BasicAstOptimizer implements AstOptimizer {

    private final DefaultAstOptimizer optimizer;

    public BasicAstOptimizer(final BasicTypeManager typeManager, final SymbolTable symbolTable) {
        this.optimizer = new DefaultAstOptimizer(
                new DefaultAstExpressionOptimizer(typeManager),
                BasicAstOptimizer::condition,
                List.of(
                        new StatementComponent<>(PrintStatement.class, BasicAstOptimizer::printStatement),
                        new StatementComponent<>(RandomizeStatement.class, BasicAstOptimizer::randomizeStatement),
                        new StatementComponent<>(SleepStatement.class, BasicAstOptimizer::sleepStatement)
                ),
                symbolTable
        );
    }

    @Override
    public AstProgram program(final AstProgram program) {
        return optimizer.program(program);
    }

    @Override
    public AstExpressionOptimizer expressionOptimizer() {
        return optimizer.expressionOptimizer();
    }

    /**
     * Replaces the condition 'x != 0' with just 'x', because a BASIC condition is an integer that is
     * true when non-zero.
     */
    private static Expression condition(final Expression expression) {
        if (expression instanceof NotEqualExpression ne) {
            if (ne.getLeft().equals(ZERO)) {
                return ne.getRight();
            } else if (ne.getRight().equals(ZERO)) {
                return ne.getLeft();
            }
        }
        return expression;
    }

    /**
     * Optimizes SLEEP statements.
     */
    private static Statement sleepStatement(final SleepStatement statement, final DefaultAstOptimizer optimizer) {
        if (statement.getExpression() != null) {
            return statement.withExpression(optimizer.expression(statement.getExpression()));
        } else {
            return statement;
        }
    }

    /**
     * Optimizes RANDOMIZE statements.
     */
    private static Statement randomizeStatement(final RandomizeStatement statement,
                                                final DefaultAstOptimizer optimizer) {
        if (statement.getExpression() != null) {
            return statement.withExpression(optimizer.expression(statement.getExpression()));
        } else {
            return statement;
        }
    }

    /**
     * Optimizes PRINT statements.
     */
    private static Statement printStatement(final PrintStatement statement, final DefaultAstOptimizer optimizer) {
        return statement.withExpressions(optimizer.expressions(statement.getExpressions()));
    }
}
