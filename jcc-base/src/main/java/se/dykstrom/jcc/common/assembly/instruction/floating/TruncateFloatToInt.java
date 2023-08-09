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

import se.dykstrom.jcc.common.assembly.base.Instruction;

/**
 * Base class for all "cvttsd2si" floating point instructions. The "cvttsd2si" instruction truncates
 * a floating point value stored in a register or memory to an integer value stored in a general
 * purpose register. If register XMM0 contains the value "3.54", the instruction
 * "cvttsd2si rbx, xmm0" would leave the value "3" in register RBX.
 *
 * @author Johan Dykstrom
 */
public abstract class TruncateFloatToInt implements Instruction {

    private final String source;
    private final String destination;

    TruncateFloatToInt(String source, String destination) {
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
        return "cvttsd2si " + destination + ", " + source;
    }
}
