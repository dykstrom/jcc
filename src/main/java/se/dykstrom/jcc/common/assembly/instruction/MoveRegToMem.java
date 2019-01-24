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
import se.dykstrom.jcc.common.assembly.base.Register8;

/**
 * Represents the assembly instruction of moving the contents of the source (a register) 
 * to the destination (a memory location). The memory location may be specified by a register 
 * as in "mov [rax], rbx", or by an immediate memory address as in "mov [address], rbx". 
 * The memory location may also have an additional offset, as in "mov [rax+10h], rbx".
 *
 * This class also supports moving data from 8-bit registers, as in "mov [rax], bl".
 *
 * @author Johan Dykstrom
 */
public class MoveRegToMem extends Move {

    public MoveRegToMem(Register source, String destination) {
        super(source.toString(), "[" + destination + "]");
    }

    public MoveRegToMem(Register source, Register destination) {
        super(source.toString(), "[" + destination + "]");
    }

    public MoveRegToMem(Register source, Register destination, String offset) {
        super(source.toString(), "[" + destination + "+" + offset + "]");
    }

    public MoveRegToMem(Register8 source, Register destination) {
        super(source.toString(), "[" + destination + "]");
    }
}
