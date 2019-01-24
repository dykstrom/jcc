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

import se.dykstrom.jcc.common.assembly.base.OperandSize;
import se.dykstrom.jcc.common.assembly.base.Register;

/**
 * Represents the assembly instruction of moving an immediate value  to the destination (a memory location).
 * The memory location may be specified by a register as in "mov [rax], byte 17", or by an immediate
 * memory address as in "mov [address], byte 17". The memory location may also have an additional offset,
 * as in "mov [rax+10h], byte 17".
 *
 * @author Johan Dykstrom
 */
class MoveImmToMem extends Move {

    MoveImmToMem(String immediate, Register destination, OperandSize size) {
        super(immediate, "[" + destination + "]", size);
    }

    MoveImmToMem(String immediate, Register destination, String offset, OperandSize size) {
        super(immediate, "[" + destination + "+" + offset + "]", size);
    }
}
