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
 * Represents the assembly instruction of subtracting the contents of a register from a memory location, such as "sub [address], rax".
 *
 * @author Johan Dykstrom
 */
public class SubRegFromMem extends Sub {

    private final Register register;
    private final String memory;

    public SubRegFromMem(Register register, String memory) {
        super(register.toString(), "[" + memory + "]");
        this.register = register;
        this.memory = memory;
    }

    public Register getRegister() {
        return register;
    }

    public String getMemory() {
        return memory;
    }
}
