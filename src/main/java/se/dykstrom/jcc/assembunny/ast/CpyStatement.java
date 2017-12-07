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

import java.util.Objects;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.Statement;

/**
 * Represents a copy statement such as 'cpy a b'.
 *
 * @author Johan Dykstrom
 */
public class CpyStatement extends Statement {

    private final AssembunnyRegister destination;
    private final Expression source;

    public CpyStatement(int line, int column, Expression source, AssembunnyRegister destination) {
        this(line, column, source, destination, null);
    }

    public CpyStatement(int line, int column, Expression source, AssembunnyRegister destination, String label) {
        super(line, column, label);
        this.source = source;
        this.destination = destination;
    }

    @Override
    public String toString() {
        return "cpy " + source + " " + destination.toString().toLowerCase();
    }

    public Expression getSource() {
        return source;
    }
    
    public AssembunnyRegister getDestination() {
        return destination;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CpyStatement that = (CpyStatement) o;
        return Objects.equals(this.source, that.source) && 
               Objects.equals(this.destination, that.destination) && 
               Objects.equals(this.getLabel(), that.getLabel());
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, destination, getLabel());
    }
}
