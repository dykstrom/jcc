/*
 * Copyright (C) 2016 Johan Dykstrom
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

import static se.dykstrom.jcc.common.utils.ReflectionUtils.getConstructorOrFail;
import static se.dykstrom.jcc.common.utils.ReflectionUtils.invokeConstructorOrFail;

/**
 * The base class for all binary expressions, for example add expressions.
 *
 * @author Johan Dykstrom
 */
public abstract class BinaryExpression extends AbstractNode implements Expression {

    private final Expression left;
    private final Expression right;

    BinaryExpression(int line, int column, Expression left, Expression right) {
        super(line, column);
        this.left = left;
        this.right = right;
    }

    /**
     * Returns the left hand side expression.
     */
    public Expression getLeft() {
        return left;
    }

    /**
     * Returns a copy of this expression, with the LHS expression set to {@code left}.
     *
     * @implNote The default implementation uses reflection to find a suitable constructor
     * to create the copy.
     * @param left The new LHS expression.
     * @return A binary expression, based on this expression with the LHS expression altered.
     */
    public BinaryExpression withLeft(Expression left) {
        return withLeftAndRight(left, getRight());
    }

    /**
     * Returns the right hand side expression.
     */
    public Expression getRight() {
        return right;
    }

    /**
     * Returns a copy of this expression, with the RHS expression set to {@code right}.
     *
     * @implNote The default implementation uses reflection to find a suitable constructor
     * to create the copy.
     * @param right The new RHS expression.
     * @return A binary expression, based on this expression with the RHS expression altered.
     */
    public BinaryExpression withRight(Expression right) {
        return withLeftAndRight(getLeft(), right);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BinaryExpression that = (BinaryExpression) o;
        return Objects.equals(left, that.left) && Objects.equals(right, that.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    private BinaryExpression withLeftAndRight(Expression left, Expression right) {
        Class<? extends BinaryExpression> clazz = getClass();
        Constructor<? extends BinaryExpression> constructor =
                getConstructorOrFail(clazz, int.class, int.class, Expression.class, Expression.class);
        return invokeConstructorOrFail(constructor, line(), column(), left, right);
    }
}
