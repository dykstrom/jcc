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

/**
 * Represents an identifier defined in the source code. All identifiers are typed.
 *
 * @author Johan Dykstrom
 */
public class Identifier implements Comparable<Identifier> {

    private final String name;
    private final Type type;

    public Identifier(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return name + " : " + type.getName();
    }

    /**
     * Returns the name of the identifier.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the mapped name of the identifier, that is, the name that should be used in code generation
     * to avoid any clashes with the backend assembler reserved words.
     */
    public String getMappedName() {
        return "_" + name;
    }

    /**
     * Returns the type of the identifier.
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns a copy of this identifier, with the type set to {@code type}.
     */
    public Identifier withType(Type type) {
        return new Identifier(name, type);
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

    @Override
    public int compareTo(Identifier that) {
        return name.compareTo(that.name);
    }
}
