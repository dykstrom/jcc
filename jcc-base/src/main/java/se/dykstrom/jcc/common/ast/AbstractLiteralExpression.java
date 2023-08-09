/*
 * Copyright (C) 2021 Johan Dykstrom
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

import static java.util.Objects.requireNonNull;

/**
 * An abstract base class for literal expressions.
 *
 * @author Johan Dykstrom
 */
public abstract class AbstractLiteralExpression extends AbstractNode implements LiteralExpression {

    private final String value;
    private final Type type;

    public AbstractLiteralExpression(int line, int column, String value, Type type) {
        super(line, column);
        this.value = requireNonNull(value);
        this.type = requireNonNull(type);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractLiteralExpression that = (AbstractLiteralExpression) o;
        return value.equals(that.value) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type);
    }
}
