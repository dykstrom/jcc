/*
 * Copyright (C) 2019 Johan Dykstrom
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
import se.dykstrom.jcc.common.assembly.base.Register;

/**
 * Represents the assembly instruction of loading an address into the destination register.
 * The address is calculated using the base address/register, an optional scale, and an optional index register.
 * An example with scale would be "lea rbx, [_foo+8*rax]". Another example, using an index register would
 * be "lea rbx, [rax+rdx]".
 *
 * @author Johan Dykstrom
 */
public class Lea implements Instruction {

    private final String base;
    private final String index;
    private final String destination;
    private final int scale;

    public Lea(String baseAddress, Register destinationRegister) {
        this.base = baseAddress;
        this.scale = -1;
        this.index = null;
        this.destination = destinationRegister.toString();
    }

    public Lea(String baseAddress, int scale, Register indexRegister, Register destinationRegister) {
        this.base = baseAddress;
        this.scale = scale;
        this.index = indexRegister.toString();
        this.destination = destinationRegister.toString();
    }

    public Lea(Register baseRegister, Register indexRegister, Register destinationRegister) {
        this.base = baseRegister.toString();
        this.scale = -1;
        this.index = indexRegister.toString();
        this.destination = destinationRegister.toString();
    }

    @Override
    public String toText() {
        StringBuilder builder = new StringBuilder();
        builder.append("lea ").append(destination).append(", ");
        builder.append("[");
        builder.append(base);
        if (scale != -1) {
            builder.append("+").append(scale).append("*").append(index);
        } else if (index != null) {
            builder.append("+").append(index);
        }
        builder.append("]");
        return builder.toString();
    }
}
