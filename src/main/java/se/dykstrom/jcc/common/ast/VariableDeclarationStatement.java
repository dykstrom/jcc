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

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.joining;

/**
 * Represents one or more variable declarations, such as "DIM count AS INTEGER" in BASIC.
 *
 * @author Johan Dykstrom
 */
public class VariableDeclarationStatement extends AbstractNode implements Statement {

    private final List<Declaration> declarations;

    public VariableDeclarationStatement(int line, int column, List<Declaration> declarations) {
        super(line, column);
        this.declarations = declarations;
    }

    @Override
    public String toString() {
        return "DIM " + toString(declarations);
    }

    private String toString(List<Declaration> declarations) {
        return declarations.stream().map(Object::toString).collect(joining(", "));
    }

    /**
     * Returns the declarations of this statement.
     */
    public List<Declaration> getDeclarations() {
        return declarations;
    }

    /**
     * Returns a copy of this instance with the declarations updated.
     * The original instance remains unchanged.
     */
    public VariableDeclarationStatement withDeclarations(List<Declaration> declarations) {
        return new VariableDeclarationStatement(line(), column(), declarations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VariableDeclarationStatement that = (VariableDeclarationStatement) o;
        return Objects.equals(this.declarations, that.declarations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(declarations);
    }
}
