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

import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Type;

import java.util.Objects;

/**
 * Base class for different types of identifier expressions.
 *
 * @author Johan Dykstrom
 */
public class IdentifierExpression extends AbstractNode implements TypedExpression {

    private final Identifier identifier;

    public IdentifierExpression(int line, int column, Identifier identifier) {
        super(line, column);
        this.identifier = identifier;
    }

    /**
     * Returns the actual identifier.
     */
    public Identifier getIdentifier() {
        return identifier;
    }

    /**
     * Returns a copy of this expression, with the identifier set to {@code identifier}.
     */
    public IdentifierExpression withIdentifier(Identifier identifier) {
        return new IdentifierExpression(line(), column(), identifier);
    }

    @Override
    public Type getType() {
        return getIdentifier().type();
    }

    @Override
    public String toString() {
        return identifier.name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentifierExpression that = (IdentifierExpression) o;
        return Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }
}
