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

/**
 * Represents the assembly instruction of multiplying the contents of the source (a floating point register)
 * with the contents of the destination (another floating point register), such as "mulsd xmm0, xmm1".
 * The result is stored in the destination, being "xmm0" in the example.
 *
 * @author Johan Dykstrom
 */
public class MulFloatRegWithFloatReg extends MulFloat {

    public MulFloatRegWithFloatReg(FloatRegister source, FloatRegister destination) {
        super(source.toString(), destination.toString());
    }
}
