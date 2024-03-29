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

package se.dykstrom.jcc.common.assembly.instruction.floating;

import se.dykstrom.jcc.common.assembly.base.FloatRegister;
import se.dykstrom.jcc.common.assembly.base.Register;

/**
 * Represents the assembly instruction of moving the contents of the source (a floating point register)
 * to the destination (a memory location). The memory location may be specified by a register as in
 * "movdqu [rbp], xmm1".
 *
 * @author Johan Dykstrom
 */
public class MoveDquFloatRegToMem extends MoveDqu {

    public MoveDquFloatRegToMem(final FloatRegister source, final Register destination) {
        super(source.toString(), "[" + destination.toString() + "]");
    }
}
