/*
 * Copyright (C) 2017 Johan Dykstrom
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

package se.dykstrom.jcc.assembunny.ast;

import se.dykstrom.jcc.common.ast.AbstractNode;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.Statement;

import java.util.Objects;

/**
 * Represents a copy statement such as 'cpy a b'.
 *
 * @author Johan Dykstrom
 */
public class CpyStatement extends AbstractNode implements Statement {

    private final Expression destination;
    private final Expression source;

    public CpyStatement(final int line, final int column, final Expression source, final Expression destination) {
        super(line, column);
        this.source = source;
        this.destination = destination;
    }

    @Override
    public String toString() {
        return "cpy " + source + " " + destination;
    }

    public Expression getSource() {
        return source;
    }
    
    public Expression getDestination() {
        return destination;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CpyStatement that = (CpyStatement) o;
        return Objects.equals(this.source, that.source) && 
               Objects.equals(this.destination, that.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, destination);
    }
}
