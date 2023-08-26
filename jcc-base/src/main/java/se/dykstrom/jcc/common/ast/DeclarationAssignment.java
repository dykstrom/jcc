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

package se.dykstrom.jcc.common.ast;

import se.dykstrom.jcc.common.types.Type;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Represents a declared variable with an assigned value.
 *
 * @author Johan Dykstrom
 */
public class DeclarationAssignment extends AbstractNode {

    private final String name;
    private final Type type;
    private final Expression expression;

    public DeclarationAssignment(final int line,
                                 final int column,
                                 final String name,
                                 final Type type,
                                 final Expression expression) {
        super(line, column);
        this.name = requireNonNull(name);
        // Allow null because we may not know the type yet
        this.type = type;
        this.expression = requireNonNull(expression);
    }

    public String name() {
        return name;
    }

    public Type type() {
        return type;
    }

    public Expression expression() {
        return expression;
    }

    public DeclarationAssignment withExpression(final Expression expression) {
        return new DeclarationAssignment(line(), column(), name, type, expression);
    }

    @Override
    public String toString() {
        return name + " : " + type + " = " + expression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeclarationAssignment that = (DeclarationAssignment) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, expression);
    }
}
