/*
 * Copyright (C) 2020 Johan Dykstrom
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

import se.dykstrom.jcc.common.types.Arr;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Type;

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.joining;

/**
 * Represents accessing an array, such as 'a%(5, 6)' in Basic, or 'a[5][6]' in some other language.
 *
 * @author Johan Dykstrom
 */
public class ArrayAccessExpression extends Expression implements TypedExpression {

    private final Identifier identifier;
    private final List<Expression> subscripts;

    public ArrayAccessExpression(int line, int column, Identifier identifier, List<Expression> subscripts) {
        super(line, column);
        this.identifier = identifier;
        this.subscripts = subscripts;
    }

    @Override
    public String toString() {
        return identifier.getName() + "(" + toString(subscripts) + ")";
    }

    private String toString(List<Expression> subscripts) {
        return subscripts.stream().map(Expression::toString).collect(joining(", "));
    }

    @Override
    public Type getType() {
        return ((Arr) identifier.getType()).getElementType();
    }

    /**
     * Returns the array identifier.
     */
    public Identifier getIdentifier() {
        return identifier;
    }

    /**
     * Returns the array subscripts.
     */
    public List<Expression> getSubscripts() {
        return subscripts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayAccessExpression that = (ArrayAccessExpression) o;
        return Objects.equals(this.identifier, that.identifier) && Objects.equals(this.subscripts, that.subscripts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, subscripts);
    }
}
