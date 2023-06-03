/*
 * Copyright (C) 2017 Johan Dykstrom
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

package se.dykstrom.jcc.common.ast;

import java.lang.reflect.Constructor;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static se.dykstrom.jcc.common.utils.ReflectionUtils.getConstructorOrFail;
import static se.dykstrom.jcc.common.utils.ReflectionUtils.invokeConstructorOrFail;

/**
 * Base class for all unary expressions, for example NOT expressions.
 *
 * @author Johan Dykstrom
 */
public abstract class UnaryExpression extends AbstractNode implements Expression {

    private final Expression expression;

    protected UnaryExpression(int line, int column, Expression expression) {
        super(line, column);
        this.expression = requireNonNull(expression);
    }

    public Expression getExpression() {
        return expression;
    }

    /**
     * Returns a copy of this expression, with the subexpression set to {@code expression}.
     *
     * @implNote The default implementation uses reflection to find a suitable constructor
     * to create the copy.
     * @param expression The new subexpression.
     * @return A unary expression, based on this expression with the subexpression altered.
     */
    public Expression withExpression(Expression expression) {
        Class<? extends UnaryExpression> clazz = getClass();
        Constructor<? extends UnaryExpression> constructor =
                getConstructorOrFail(clazz, int.class, int.class, Expression.class);
        return invokeConstructorOrFail(constructor, line(), column(), expression);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnaryExpression that = (UnaryExpression) o;
        return Objects.equals(this.expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression);
    }
}
