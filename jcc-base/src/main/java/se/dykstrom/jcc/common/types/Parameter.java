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
 * Represents a parameter to a function. A parameter behaves much like an identifier,
 * but is stored on the stack.
 */
public class Parameter extends Identifier {

    private final String homeLocation;

    public Parameter(final String name, final Type type, final String homeLocation) {
        super(name, type);
        this.homeLocation = requireNonNull(homeLocation);
    }

    @Override
    public String getMappedName() {
        return homeLocation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Parameter parameter = (Parameter) o;
        return Objects.equals(homeLocation, parameter.homeLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), homeLocation);
    }
}
