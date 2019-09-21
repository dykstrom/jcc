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

package se.dykstrom.jcc.common.optimization;

import se.dykstrom.jcc.common.ast.Expression;

/**
 * Interface to be implemented by optimizers that do optimization on expressions in the abstract syntax tree.
 *
 * @author Johan Dykstrom
 */
public interface AstExpressionOptimizer {
    /**
     * Returns a copy of the given expression, where some parts of the syntax tree may be have been optimized.
     *
     * @param expression The original expression.
     * @return The optimized expression.
     */
    Expression expression(Expression expression);
}
