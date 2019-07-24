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
 * Represents the assembly instruction of loading an address into the destination register.
 * The address is calculated using the base register, an optional index register, and an optional offset.
 * An example with offset would be "lea rbx, [rax+10h]". Another example, using an index register would
 * be "lea rbx, [rax+rdx]".
 *
 * @author Johan Dykstrom
 */
public class Lea implements Instruction {

    private final String baseRegister;
    private final String indexRegister;
    private final String offset;
    private final String destinationRegister;

    public Lea(Register baseRegister, String offset, Register destinationRegister) {
        this.baseRegister = baseRegister.toString();
        this.indexRegister = null;
        this.offset = offset;
        this.destinationRegister = destinationRegister.toString();
    }

    public Lea(Register baseRegister, Register indexRegister, Register destinationRegister) {
        this.baseRegister = baseRegister.toString();
        this.indexRegister = indexRegister.toString();
        this.offset = null;
        this.destinationRegister = destinationRegister.toString();
    }

    @Override
    public String toAsm() {
        return "lea " + destinationRegister + ", "
                + "["
                + baseRegister
                + (indexRegister != null ? "+" + indexRegister : "")
                + (offset != null ? "+" + offset : "")
                + "]";
    }
}
