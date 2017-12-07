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
 * Represents the assembly instruction of comparing the contents of operand 1 (a register) with 
 * the contents of operand 2 (a memory location). The memory location may be specified by a register 
 * as in "cmp rbx, [rax]", or by an immediate memory address as in "cmp rbx, [address]". The 
 * memory location may also have an additional offset, as in "cmp rbx, [rax+10h]".
 *
 * @author Johan Dykstrom
 */
public class CmpRegWithMem extends Cmp {

    public CmpRegWithMem(Register operand1, String operand2) {
        super(operand1.toString(), "[" + operand2 + "]");
    }

    public CmpRegWithMem(Register operand1, Register operand2, String offset) {
        super(operand1.toString(), "[" + operand2.toString() + "+" + offset + "]");
    }
}
