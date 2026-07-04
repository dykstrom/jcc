/*
 * Copyright (C) 2023 Johan Dykstrom
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

package se.dykstrom.jcc.col.ast.statement;

import se.dykstrom.jcc.common.ast.AbstractNode;
import se.dykstrom.jcc.common.ast.FunctionCallExpression;
import se.dykstrom.jcc.common.ast.Statement;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Represents a function call that is not part of an expression, such as 'sleep(100)'.
 *
 * @author Johan Dykstrom
 */
public class FunCallStatement extends AbstractNode implements Statement {

    private final FunctionCallExpression expression;
    /** Whether the call was written with the leading 'call' keyword; a bare call is a semantic error. */
    private final boolean hasCall;

    public FunCallStatement(final int line, final int column, final FunctionCallExpression expression, final boolean hasCall) {
        super(line, column);
        this.expression = requireNonNull(expression);
        this.hasCall = hasCall;
    }

    public FunCallStatement(final int line, final int column, final FunctionCallExpression expression) {
        this(line, column, expression, true);
    }

    public FunCallStatement(final FunctionCallExpression expression) {
        this(0, 0, expression, true);
    }

    @Override
    public String toString() {
        return (hasCall ? "call " : "") + expression.toString();
    }

    public FunctionCallExpression expression() {
        return expression;
    }

    public boolean hasCall() {
        return hasCall;
    }

    /**
     * Returns a copy of this statement, with the expression set to {@code expression}.
     */
    public FunCallStatement withExpression(final FunctionCallExpression expression) {
        return new FunCallStatement(line(), column(), expression, hasCall);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final FunCallStatement that = (FunCallStatement) o;
        return hasCall == that.hasCall && Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, hasCall);
    }
}
