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

package se.dykstrom.jcc.common.assembly.instruction;

import java.util.Objects;

import se.dykstrom.jcc.common.assembly.base.Instruction;

/**
 * Base class for all "call" instructions.
 *
 * @author Johan Dykstrom
 */
public abstract class Call implements Instruction {

    private final String target;

    Call(String target) {
        this.target = target;
    }

    /**
     * Returns the target of the call.
     */
    public String getTarget() {
        return target;
    }

    @Override
    public String toText() {
        return "call " + target;
    }

    @Override
    public String toString() {
        return "call " + target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Call that = (Call) o;
        return Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target);
    }
}
