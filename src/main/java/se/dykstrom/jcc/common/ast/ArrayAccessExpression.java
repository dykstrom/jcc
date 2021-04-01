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

import se.dykstrom.jcc.common.types.*;

import java.util.*;

import static java.util.stream.Collectors.joining;

/**
 * Represents accessing an array, such as 'a%(5, 6)' in Basic, or 'a[5][6]' in some other language.
 *
 * @author Johan Dykstrom
 */
public class ArrayAccessExpression extends IdentifierDerefExpression {

    private final List<Expression> subscripts;

    public ArrayAccessExpression(int line, int column, Identifier identifier, List<Expression> subscripts) {
        super(line, column, identifier);
        this.subscripts = subscripts;
        assert identifier.getType() instanceof Arr : "expected array identifier, but found " + identifier.getType().getName();
        assert !subscripts.isEmpty() : "empty subscripts not allowed";
        assert subscripts.size() == ((Arr) identifier.getType()).getDimensions() : "number of subscripts (" + subscripts.size()
                    + ") != number of dimensions (" + ((Arr) identifier.getType()).getDimensions() + ")";
    }

    @Override
    public String toString() {
        return getIdentifier().getName() + "(" + toString(subscripts) + ")";
    }

    private String toString(List<Expression> subscripts) {
        return subscripts.stream().map(Expression::toString).collect(joining(", "));
    }

    @Override
    public Type getType() {
        return ((Arr) getIdentifier().getType()).getElementType();
    }

    /**
     * Returns a copy of this expression, with the identifier set to {@code identifier}.
     */
    @Override
    public ArrayAccessExpression withIdentifier(Identifier identifier) {
        return new ArrayAccessExpression(getLine(), getColumn(), identifier, subscripts);
    }

    /**
     * Returns the array subscripts.
     */
    public List<Expression> getSubscripts() {
        return subscripts;
    }

    /**
     * Returns a copy of this expression, with the subscripts set to {@code subscripts}.
     */
    public ArrayAccessExpression withSubscripts(List<Expression> subscripts) {
        return new ArrayAccessExpression(getLine(), getColumn(), getIdentifier(), subscripts);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArrayAccessExpression that = (ArrayAccessExpression) o;
        return subscripts.equals(that.subscripts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subscripts);
    }
}
