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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.joining;

/**
 * Represents one or more constant declarations, such as "CONST MAX = 50" in BASIC.
 *
 * @author Johan Dykstrom
 */
public class ConstDeclarationStatement extends AbstractNode implements Statement {

    private final List<DeclarationAssignment> declarations;

    public ConstDeclarationStatement(final int line, final int column, final List<DeclarationAssignment> declarations) {
        super(line, column);
        this.declarations = new ArrayList<>(declarations);
    }

    @Override
    public String toString() {
        return "CONST " + toString(declarations);
    }

    private String toString(final List<DeclarationAssignment> declarations) {
        return declarations.stream().map(Object::toString).collect(joining(", "));
    }

    /**
     * Returns the declarations of this statement.
     */
    public List<DeclarationAssignment> getDeclarations() {
        return declarations;
    }

    /**
     * Returns a copy of this instance with the declarations updated.
     * The original instance remains unchanged.
     */
    public ConstDeclarationStatement withDeclarations(final List<DeclarationAssignment> declarations) {
        return new ConstDeclarationStatement(line(), column(), declarations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstDeclarationStatement that = (ConstDeclarationStatement) o;
        return Objects.equals(this.declarations, that.declarations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(declarations);
    }
}
