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

package se.dykstrom.jcc.col.ast.statement;

import se.dykstrom.jcc.common.ast.AbstractNode;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.types.Type;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Represents an opaque type declaration such as 'type Meters as f64'.
 */
public class TypeDefStatement extends AbstractNode implements Statement {

    private final String name;
    private final Type type;

    public TypeDefStatement(final int line, final int column, final String name, final Type type) {
        super(line, column);
        this.name = requireNonNull(name);
        this.type = requireNonNull(type);
    }

    public TypeDefStatement(final String name, final Type type) {
        this(0, 0, name, type);
    }

    @Override
    public String toString() {
        return "type " + name + " as " + type;
    }

    public String name() {
        return name;
    }

    /**
     * Returns the underlying type.
     */
    public Type type() {
        return type;
    }

    public TypeDefStatement withType(final Type type) {
        return new TypeDefStatement(line(), column(), name, type);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TypeDefStatement that = (TypeDefStatement) o;
        return Objects.equals(name, that.name) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
}
