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

import static se.dykstrom.jcc.common.assembly.base.OperandSize.BYTE;

/**
 * Represents the assembly instruction of moving a byte from a memory location to a register with
 * zero extension. The memory location may be specified by a register as in "movzx rbx, byte [rax]",
 * or by an immediate memory address as in "movzx rbx, byte [address]".
 *
 * @author Johan Dykstrom
 */
public class MoveByteMemToReg extends MoveWithZeroExtend {

    public MoveByteMemToReg(Register source, Register destination) {
        super("[" + source.toString() + "]", destination.toString(), BYTE);
    }
}
