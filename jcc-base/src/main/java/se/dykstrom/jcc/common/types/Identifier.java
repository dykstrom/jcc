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

package se.dykstrom.jcc.common.types;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Represents an identifier defined in the source code. All identifiers are typed.
 * But the type may not be known when creating the Identifier, so we allow it to
 * be null.
 *
 * @author Johan Dykstrom
 */
public record Identifier(String name, Type type) implements Comparable<Identifier> {

    public Identifier(final String name, final Type type) {
        this.name = requireNonNull(name);
        this.type = type;
    }

    /**
     * Returns the mapped name of the identifier, that is, the name that should be used in code generation
     * to avoid any clashes with the backend assembler reserved words.
     */
    public String getMappedName() {
        // Flat assembler does not allow # in identifiers
        return "_" + name.replace("#", "_hash") + (type instanceof Arr ? Arr.SUFFIX : "");
    }

    /**
     * Returns a copy of this identifier, with the type set to {@code type}.
     */
    public Identifier withType(final Type type) {
        return new Identifier(name, type);
    }

    @Override
    public String toString() {
        return name + " : " + (type != null ? type.getName() : "Unknown");
    }

    @Override
    public int compareTo(Identifier that) {
        return name.compareTo(that.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Identifier that = (Identifier) o;
        return Objects.equals(name, that.name) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
}
