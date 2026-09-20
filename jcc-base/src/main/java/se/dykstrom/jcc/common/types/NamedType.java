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

package se.dykstrom.jcc.common.types;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Represents the name of a type, that is used before resolving the actual type. This is the one
 * type that comes straight from source code, so it also carries the position of the name, to let
 * a diagnostic about an unknown or unsupported type name point at the name itself. Like the AST
 * nodes, it compares equal regardless of position.
 */
public final class NamedType implements Type {

    private final String name;
    private final int line;
    private final int column;

    public NamedType(final String name, final int line, final int column) {
        this.name = requireNonNull(name);
        this.line = line;
        this.column = column;
    }

    public NamedType(final String name) {
        this(name, 0, 0);
    }

    public String name() {
        return name;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String llvmName() {
        throw new UnsupportedOperationException("named");
    }

    @Override
    public String llvmDefaultValue() {
        throw new UnsupportedOperationException("named");
    }

    @Override
    public String getDefaultValue() {
        throw new UnsupportedOperationException("named");
    }

    @Override
    public String getFormat() {
        throw new UnsupportedOperationException("named");
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(name, ((NamedType) o).name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
