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
 * Represents the assembly instruction of converting an integer stored in the source
 * (a general purpose register) to a floating point value stored in the destination
 * (a floating point register). For example, this could be "cvtsi2sd xmm0, rbx".
 *
 * @author Johan Dykstrom
 */
public class ConvertIntRegToFloatReg extends ConvertIntToFloat {

    public ConvertIntRegToFloatReg(Register source, FloatRegister destination) {
        super(source.toString(), destination.toString());
    }
}
