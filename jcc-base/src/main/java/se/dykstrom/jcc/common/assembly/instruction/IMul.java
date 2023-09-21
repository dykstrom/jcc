/*
 * Copyright (C) 2016 Johan Dykstrom
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

/**
 * Base class for all "imul" (signed multiplication) instructions.
 *
 * @author Johan Dykstrom
 */
public abstract class IMul implements Instruction {

    private final String source;
    private final String destination;

    IMul(String source, String destination) {
        this.source = source;
        this.destination = destination;
    }

    @Override
    public String toText() {
        return "imul " + destination + ", " + source;
    }
}
