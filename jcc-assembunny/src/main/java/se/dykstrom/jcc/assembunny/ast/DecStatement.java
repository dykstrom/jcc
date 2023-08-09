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
import se.dykstrom.jcc.common.ast.Statement;

import java.util.Objects;

/**
 * Represents a decrement statement such as 'dec a'.
 *
 * @author Johan Dykstrom
 */
public class DecStatement extends AbstractNode implements Statement {

    private final AssembunnyRegister register;

    public DecStatement(int line, int column, AssembunnyRegister register) {
        super(line, column);
        this.register = register;
    }

    @Override
    public String toString() {
        return "dec " + register.toString().toLowerCase();
    }

    public AssembunnyRegister getRegister() {
        return register;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DecStatement that = (DecStatement) o;
        return Objects.equals(this.register, that.register);
    }

    @Override
    public int hashCode() {
        return Objects.hash(register);
    }
}
