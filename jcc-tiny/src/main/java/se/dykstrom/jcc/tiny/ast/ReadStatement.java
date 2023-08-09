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

package se.dykstrom.jcc.tiny.ast;

import se.dykstrom.jcc.common.ast.AbstractNode;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.ast.Statement;

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.joining;

/**
 * Represents a read statement such as 'READ value'.
 *
 * @author Johan Dykstrom
 */
public class ReadStatement extends AbstractNode implements Statement {

    private final List<Identifier> identifiers;

    public ReadStatement(int line, int column, List<Identifier> identifiers) {
        super(line, column);
        this.identifiers = identifiers;
    }

    @Override
    public String toString() {
        return "READ " + toString(identifiers);
    }

    private String toString(List<Identifier> identifiers) {
        return identifiers.stream().map(Identifier::name).collect(joining(", "));
    }

    public List<Identifier> getIdentifiers() {
        return identifiers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReadStatement that = (ReadStatement) o;
        return Objects.equals(identifiers, that.identifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifiers);
    }
}
