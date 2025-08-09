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
import se.dykstrom.jcc.basic.compiler.BasicTypeManager;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.optimization.DefaultAstExpressionOptimizer;
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer;
import se.dykstrom.jcc.common.symbols.SymbolTable;

/**
 * The BASIC AST optimizer performs BASIC specific optimizations on the AST.
 *
 * @author Johan Dykstrom
 */
public class BasicAstOptimizer extends DefaultAstOptimizer {

    public BasicAstOptimizer(final BasicTypeManager typeManager, final SymbolTable symbolTable) {
        super(new DefaultAstExpressionOptimizer(typeManager), symbolTable);
    }

    @Override
    protected Statement statement(final Statement statement) {
        return switch (statement) {
            case PrintStatement ps -> printStatement(ps);
            case RandomizeStatement rs -> randomizeStatement(rs);
            case SleepStatement ss -> sleepStatement(ss);
            default -> super.statement(statement);
        };
    }

    /**
     * Optimizes SLEEP statements.
     */
    private Statement sleepStatement(final SleepStatement statement) {
        if (statement.getExpression() != null) {
            return statement.withExpression(expression(statement.getExpression()));
        } else {
            return statement;
        }
    }

    /**
     * Optimizes RANDOMIZE statements.
     */
    private Statement randomizeStatement(final RandomizeStatement statement) {
        if (statement.getExpression() != null) {
            return statement.withExpression(expression(statement.getExpression()));
        } else {
            return statement;
        }
    }

    /**
     * Optimizes PRINT statements.
     */
    private Statement printStatement(final PrintStatement statement) {
        return statement.withExpressions(statement.getExpressions().stream().map(this::expression).toList());
    }
}
