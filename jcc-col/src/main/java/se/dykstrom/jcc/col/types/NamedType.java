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

package se.dykstrom.jcc.col.types;

import se.dykstrom.jcc.common.types.Type;

import static java.util.Objects.requireNonNull;

/**
 * Represents the name of a type, that is used before resolving the actual type.
 */
public record NamedType(String name) implements Type {

    public NamedType(final String name) {
        this.name = requireNonNull(name);
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
    public String getDefaultValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getFormat() {
        throw new UnsupportedOperationException();
    }
}
