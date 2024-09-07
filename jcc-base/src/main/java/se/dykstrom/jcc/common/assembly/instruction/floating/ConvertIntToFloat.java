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

import se.dykstrom.jcc.common.assembly.instruction.Instruction;

/**
 * Base class for all "cvtsi2sd" floating point instructions. The "cvtsi2sd" instruction converts
 * an integer stored in a general purpose register or memory to a floating point value stored in
 * a floating point register. If register RBX contains the integer value "3", the instruction
 * "cvtsi2sd xmm0, rbx" would leave the value "3.0" in register XMM0.
 *
 * @author Johan Dykstrom
 */
abstract class ConvertIntToFloat implements Instruction {

    private final String source;
    private final String destination;

    ConvertIntToFloat(String source, String destination) {
        this.destination = destination;
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    @Override
    public String toText() {
        return "cvtsi2sd " + destination + ", " + source;
    }
}
