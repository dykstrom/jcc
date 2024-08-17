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

/**
 * Base class for all "sal" (shift arithmetic left) instructions.
 *
 * @author Johan Dykstrom
 */
public abstract class Sal implements Instruction {

    private final String first;
    private final String second;

    Sal(String first, String second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String toText() {
        return "sal " + first + ", " + second;
    }
}
