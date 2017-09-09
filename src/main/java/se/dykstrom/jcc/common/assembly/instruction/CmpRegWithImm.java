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
 * Represents the assembly instruction of comparing the contents of a register with an immediate value,
 * such as "cmp rax, 0".
 *
 * @author Johan Dykstrom
 */
public class CmpRegWithImm extends Cmp {

    private final Register register;
    private final String immediate;

    public CmpRegWithImm(Register register, String immediate) {
        super(register.toString(), immediate);
        this.register = register;
        this.immediate = immediate;
    }

    public Register getRegister() {
        return register;
    }

    public String getImmediate() {
        return immediate;
    }
}