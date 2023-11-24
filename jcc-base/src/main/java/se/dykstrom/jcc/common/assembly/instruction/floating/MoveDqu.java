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

import static java.util.Objects.requireNonNull;

/**
 * Base class for all "movdqu" (Move Unaligned Packed Integer Values) instructions.
 * The "movdqu" instruction moves data between a 128-bit xmm register and memory location.
 * Compare this to the "movsd" instruction {@link MoveFloat} that moves 64-bit data.
 *
 * @author Johan Dykstrom
 */
abstract class MoveDqu implements Instruction {

    private final String source;
    private final String destination;

    MoveDqu(final String source, final String destination) {
        this.destination = requireNonNull(destination);
        this.source = requireNonNull(source);
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    @Override
    public String toText() {
        return "movdqu " + destination + ", " + source;
    }
}
