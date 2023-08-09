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

/**
 * Represents the assembly instruction of signed integer division where the dividend is always registers rdx:rax,
 * and the divisor is given by a memory location. For example, this could be "idiv [address]".
 *
 * @author Johan Dykstrom
 */
public class IDivWithMem extends IDiv {

    public IDivWithMem(final String memory) {
        super("[" + memory + "]");
    }
}
