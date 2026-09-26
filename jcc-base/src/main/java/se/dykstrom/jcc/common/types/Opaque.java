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

package se.dykstrom.jcc.common.types;

import static java.util.Objects.requireNonNull;

/**
 * Represents an opaque type: a distinct type with the same representation as its underlying type.
 * Identity is nominal, so two opaque types are equal only if they have the same name, and an
 * opaque type is never equal to its underlying type.
 *
 * <p>This type deliberately does not report itself as numeric, even over a numeric underlying
 * type, so that the numeric promotion rules never apply to it.
 */
public record Opaque(String name, Type underlying) implements Type {

    public Opaque {
        requireNonNull(name);
        requireNonNull(underlying);
    }

    /**
     * Returns the underlying type if {@code type} is an opaque type, and {@code type} otherwise.
     */
    public static Type unwrap(final Type type) {
        return (type instanceof Opaque opaque) ? opaque.underlying() : type;
    }

    /**
     * Returns the internal name, which is used to mangle function names. The prefix keeps an opaque
     * type named like a built-in type's internal name, for example I64, from mangling to the same name.
     */
    @Override
    public String getName() {
        return "Opaque." + name;
    }

    @Override
    public String llvmName() {
        return underlying.llvmName();
    }

    @Override
    public String llvmDefaultValue() {
        return underlying.llvmDefaultValue();
    }

    @Override
    public String getDefaultValue() {
        return underlying.getDefaultValue();
    }

    @Override
    public String getFormat() {
        return underlying.getFormat();
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(final Object o) {
        return (o instanceof Opaque that) && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
