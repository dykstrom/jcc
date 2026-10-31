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

package se.dykstrom.jcc.common.optimization;

import se.dykstrom.jcc.common.ast.Statement;

/**
 * Optimizes one type of statement. A language adds a statement optimizer for each statement of
 * its own that has expressions to optimize, when it creates its {@link DefaultAstOptimizer}.
 * The optimizer gets the {@link DefaultAstOptimizer} rather than an {@link AstOptimizer}, because
 * it needs {@link DefaultAstOptimizer#expression} and {@link DefaultAstOptimizer#expressions},
 * which optimize expressions without a symbol table.
 */
@FunctionalInterface
public interface StatementOptimizer<T extends Statement> {

    /**
     * Returns an optimized copy of the given statement.
     *
     * @param statement The statement to optimize.
     * @param optimizer The AST optimizer, used to optimize nested expressions and statements.
     * @return The optimized statement.
     */
    Statement optimize(T statement, DefaultAstOptimizer optimizer);
}
