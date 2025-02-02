/*
 * Copyright (C) 2024 Johan Dykstrom
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
 * Base class for all "roundsd" floating point instructions. The "roundsd" instruction converts
 * a floating point value stored in a register or memory to an integer value stored in a floating
 * point register using the specified rounding mode. If register XMM0 contains the value "3.54",
 * the instruction "roundsd xmm0, xmm0, 1000b" would leave the value "4.00" in register XMM0,
 * while "roundsd xmm0, xmm0, 1011b" would leave the value "3.00" in register XMM0
 *
 * @author Johan Dykstrom
 */
public abstract class ConvertFloatToFloat implements Instruction {

    private final String source;
    private final String destination;
    private final String roundingMode;

    ConvertFloatToFloat(final String source, final String destination, final String roundingMode) {
        this.source = source;
        this.destination = destination;
        this.roundingMode = roundingMode;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    @Override
    public String toText() {
        return "roundsd " + destination + ", " + source + ", " + roundingMode;
    }
}
