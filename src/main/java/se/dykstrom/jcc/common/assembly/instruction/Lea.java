/*
 * Copyright (C) 2019 Johan Dykstrom
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

import se.dykstrom.jcc.common.assembly.base.Instruction;
import se.dykstrom.jcc.common.assembly.base.Register;

/**
 * Represents the assembly instruction of loading an address into the destination (a register).
 * The address is created using the source (a register) and an optional offset. An example would be
 * "lea rbx, [rax+10h]".
 *
 * @author Johan Dykstrom
 */
public class Lea implements Instruction {

    private final String source;
    private final String offset;
    private final String destination;

    public Lea(Register source, String offset, Register destination) {
        this.source = source.toString();
        this.offset = offset;
        this.destination = destination.toString();
    }

    @Override
    public String toAsm() {
        return "lea " + destination + ", [" + source + (offset != null ? "+" + offset : "") + "]";
    }
}
