/*
 * Copyright (C) 2018 Johan Dykstrom
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

import static se.dykstrom.jcc.common.assembly.base.OperandSize.BYTE;

/**
 * Represents the assembly instruction of moving a byte of an immediate value to the destination
 * (a memory location). The destination must be specified by a register, and may include an offset,
 * as in "mov [rax+10h], byte 17".
 *
 * @author Johan Dykstrom
 */
public class MoveByteImmToMem extends MoveImmToMem {

    public MoveByteImmToMem(String immediate, Register destination) {
        super(immediate, destination, BYTE);
    }

    public MoveByteImmToMem(String immediate, Register destination, String offset) {
        super(immediate, destination, offset, BYTE);
    }
}
