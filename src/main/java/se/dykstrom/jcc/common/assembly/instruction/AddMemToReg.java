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

package se.dykstrom.jcc.common.assembly.instruction;

import se.dykstrom.jcc.common.assembly.base.Register;

/**
 * Represents the assembly instruction of adding the contents of the source (a memory location) to the
 * destination (a register). The result is stored in the destination. The memory location may be specified 
 * by a register as in "add rbx, [rax]", or by an immediate memory address as in "add rbx, [address]". The 
 * memory location may also have an additional offset, as in "add rbx, [rax+10h]".
 *
 * @author Johan Dykstrom
 */
public class AddMemToReg extends Add {

    public AddMemToReg(String source, Register destination) {
        super("[" + source + "]", destination.toString());
    }

    public AddMemToReg(Register source, String offset, Register destination) {
        super("[" + source + "+" + offset + "]", destination.toString());
    }
}
