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

import se.dykstrom.jcc.common.assembly.base.Instruction;
import se.dykstrom.jcc.common.assembly.base.OperandSize;

/**
 * Base class for all "movzx" instructions.
 *
 * @author Johan Dykstrom
 */
abstract class MoveWithZeroExtend implements Instruction {

    private final String source;
    private final String destination;
    private final OperandSize size;

    /**
     * Creates a new movzx instruction.
     * 
     * @param source Source operand.
     * @param destination Destination operand.
     * @param size A size specifier, for example BYTE.
     */
    MoveWithZeroExtend(String source, String destination, OperandSize size) {
        this.destination = destination;
        this.source = source;
        this.size = size;
    }

    @Override
    public String toText() {
        return "movzx " + destination + ", " + size + " " + source;
    }
}
