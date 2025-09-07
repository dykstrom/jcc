/*
 * Copyright (C) 2018 Johan Dykstrom
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
 * Represents a declared variable.
 *
 * @author Johan Dykstrom
 */
public class Declaration extends AbstractNode {

    private final String name;
    private final Type type;

    public Declaration(final int line, final int column, final String name, final Type type) {
        super(line, column);
        this.name = requireNonNull(name);
        this.type = requireNonNull(type);
    }

    public Declaration(final String name, final Type type) {
        this(0, 0, name, type);
    }

    public String name() {
        return name;
    }

    public Type type() {
        return type;
    }

    public Declaration withType(final Type type) {
        return new Declaration(line(), column(), name, type);
    }

    @Override
    public String toString() {
        return name + " : " + type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Declaration that = (Declaration) o;
        return Objects.equals(this.name, that.name) && Objects.equals(this.type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }
}
