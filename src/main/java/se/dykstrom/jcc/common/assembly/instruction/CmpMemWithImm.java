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

import se.dykstrom.jcc.common.assembly.base.OperandSize;
import se.dykstrom.jcc.common.assembly.base.Register;

/**
 * Represents the assembly instruction of comparing the contents of a memory location with
 * the contents of an immediate value, such as "cmp [rax], byte 0". The memory location may
 * also have an offset, as in "cmp [rax+10h], byte 17".
 *
 * @author Johan Dykstrom
 */
class CmpMemWithImm extends Cmp {

    CmpMemWithImm(Register address, String immediate, OperandSize size) {
        super("[" + address + "]", size + " " + immediate);
        size.validate(immediate);
    }

    CmpMemWithImm(Register address, String offset, String immediate, OperandSize size) {
        super("[" + address + "+" + offset + "]", size + " " + immediate);
        size.validate(immediate);
    }
}
