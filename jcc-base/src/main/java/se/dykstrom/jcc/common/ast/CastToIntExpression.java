/*
 * Copyright (C) 2025 Johan Dykstrom
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

import se.dykstrom.jcc.common.types.Type;

import java.util.Objects;

/**
 * Represents a cast to an integer type.
 *
 * @author Johan Dykstrom
 */
public class CastToIntExpression extends UnaryExpression implements TypedExpression {

    private final Type type;

    public CastToIntExpression(final int line,
                               final int column,
                               final Expression expression,
                               final Type destinationType) {
        super(line, column, expression);
        this.type = destinationType;
    }

    public CastToIntExpression(final Expression expression, final Type destinationType) {
        this(0, 0, expression, destinationType);
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public Expression withExpression(final Expression expression) {
        return new CastToIntExpression(line(), column(), expression, type);
    }

    @Override
    public String toString() {
        return type.toString().toLowerCase() + "(" + getExpression() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CastToIntExpression that = (CastToIntExpression) o;
        return Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type);
    }
}
