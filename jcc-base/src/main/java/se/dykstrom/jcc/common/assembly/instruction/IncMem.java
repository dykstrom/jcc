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

/**
 * Represents an assembly "inc" instruction, incrementing the value of a memory location.
 * Memory operands must be prefixed with a size specification, like "qword".
 *
 * @author Johan Dykstrom
 */
public class IncMem extends Inc {

    private final String memory;

    public IncMem(String memory) {
        super("qword [" + memory + "]");
        this.memory = memory;
    }

    public String getMemory() {
        return memory;
    }
}
