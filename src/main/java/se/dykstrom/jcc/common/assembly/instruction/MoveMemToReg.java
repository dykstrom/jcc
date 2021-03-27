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

import se.dykstrom.jcc.common.assembly.base.Register;

/**
 * Represents the assembly instruction of moving the contents of the source (a memory location)
 * to the destination (a register). The memory location may be specified in several ways:
 *
 * <ulist>
 *   <li>A register as in "mov rbx, [rax]"</li>
 *   <li>An immediate memory address as in "mov rbx, [address]"</li>
 *   <li>A register and an additional offset, as in "mov rbx, [rax + 10h]"</li>
 *   <li>An immediate memory address and a scaled offset, as in "mov rbx, [address + 8h * rcx]"</li>
 * </ulist>
 *
 * @author Johan Dykstrom
 */
public class MoveMemToReg extends Move {

    public MoveMemToReg(String source, Register destination) {
        super("[" + source + "]", destination.toString());
    }

    public MoveMemToReg(String source, int scale, Register offset, Register destination) {
        super("[" + source + "+" + scale + "*" + offset + "]", destination.toString());
    }

    public MoveMemToReg(Register source, Register destination) {
        super("[" + source.toString() + "]", destination.toString());
    }

    public MoveMemToReg(Register source, String offset, Register destination) {
        super("[" + source.toString() + "+" + offset + "]", destination.toString());
    }
}
