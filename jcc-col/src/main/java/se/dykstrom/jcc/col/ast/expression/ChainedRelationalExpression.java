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

package se.dykstrom.jcc.col.ast.expression;

import se.dykstrom.jcc.common.ast.AbstractNode;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.TypedExpression;
import se.dykstrom.jcc.common.types.Bool;
import se.dykstrom.jcc.common.types.Type;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Marks a relational expression synthesized from an unparenthesized chain, such as '1 &lt; 2 &lt; 3'.
 * Relational operators cannot be chained; this is rejected during semantic analysis. The marker is
 * keyed on parse shape (an unparenthesized relational as the left operand), not on operand types,
 * so legitimately type-correct forms like '(a == b) == c' are not flagged.
 *
 * @author Johan Dykstrom
 */
public class ChainedRelationalExpression extends AbstractNode implements TypedExpression {

    private final Expression expression;

    public ChainedRelationalExpression(final int line, final int column, final Expression expression) {
        super(line, column);
        this.expression = requireNonNull(expression);
    }

    public ChainedRelationalExpression(final Expression expression) {
        this(0, 0, expression);
    }

    /**
     * A relational expression would be of type bool, were chaining allowed.
     */
    @Override
    public Type type() {
        return Bool.INSTANCE;
    }

    /**
     * Returns the wrapped relational expression.
     */
    public Expression expression() {
        return expression;
    }

    @Override
    public String toString() {
        return expression.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChainedRelationalExpression that = (ChainedRelationalExpression) o;
        return Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression);
    }
}
