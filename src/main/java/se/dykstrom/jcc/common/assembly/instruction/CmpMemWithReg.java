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
 * Represents the assembly instruction of comparing the contents of a memory location with the contents of a register,
 * such as "cmp [address], rax".
 *
 * @author Johan Dykstrom
 */
public class CmpMemWithReg extends Cmp {

    private final String memory;
    private final Register register;

    public CmpMemWithReg(String memory, Register register) {
        super("[" + memory + "]", register.toString());
        this.memory = memory;
        this.register = register;
    }

    public String getMemory() {
        return memory;
    }

    public Register getRegister() {
        return register;
    }
}
