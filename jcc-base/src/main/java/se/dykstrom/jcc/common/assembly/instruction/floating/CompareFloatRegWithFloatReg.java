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
 * Represents the assembly instruction of comparing two floating point values - the contents of operand 1
 * (a floating point register) and the contents of operand 2 (another floating point register), such as
 * "ucomisd xmm0, xmm1".
 *
 * @author Johan Dykstrom
 */
public class CompareFloatRegWithFloatReg extends CompareFloat {

    public CompareFloatRegWithFloatReg(FloatRegister operand1, FloatRegister operand2) {
        super(operand1.toString(), operand2.toString());
    }
}
