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

package se.dykstrom.jcc.common.assembly.base;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Represents a code label.
 *
 * @author Johan Dykstrom
 */
public class Label implements Code {

    private final String name;

    public Label(String name) {
        this.name = requireNonNull(name);
    }

    /**
     * Returns the real name of the label, not the mapped name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the mapped name of the label, that is, the name that should be used in code generation
     * to avoid any clashes with the backend assembler reserved words.
     */
    public String getMappedName() {
        return "_" + name;
    }

    @Override
    public String toAsm() {
        return getMappedName() + ":";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Label label = (Label) o;
        return name.equals(label.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
