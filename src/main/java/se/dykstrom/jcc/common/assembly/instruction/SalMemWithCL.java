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

import static se.dykstrom.jcc.common.assembly.base.Register.RCX;

/**
 * Represents the assembly instruction shift arithmetic left of a memory location with the value in CL,
 * that is "sal rax, cl".
 *
 * @author Johan Dykstrom
 */
public class SalMemWithCL extends Sal {
    public SalMemWithCL(String memoryAddress) {
        super("[" + memoryAddress + "]", RCX.asLowRegister8().toString());
    }
}
