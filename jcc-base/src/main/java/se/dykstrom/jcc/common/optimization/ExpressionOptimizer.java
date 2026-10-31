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

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.symbols.SymbolTable;

/**
 * Optimizes one type of expression. A language replaces a built-in expression optimizer, or adds
 * one for an expression of its own, when it creates its {@link DefaultAstExpressionOptimizer}.
 */
@FunctionalInterface
public interface ExpressionOptimizer<T extends Expression> {

    /**
     * Returns an optimized copy of the given expression, or the expression itself if it cannot be
     * optimized. The subexpressions of a unary, binary or if-expression, and the arguments of a
     * function call, have already been optimized. An optimizer for any other expression that has
     * subexpressions must optimize them itself, with the given AST expression optimizer.
     *
     * @param expression The expression to optimize.
     * @param symbols The symbol table, used to look up constant values.
     * @param optimizer The AST expression optimizer, used to optimize subexpressions.
     * @return The optimized expression.
     */
    Expression optimize(T expression, SymbolTable symbols, AstExpressionOptimizer optimizer);
}
