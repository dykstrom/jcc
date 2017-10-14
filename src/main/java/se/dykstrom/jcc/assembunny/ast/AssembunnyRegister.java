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

package se.dykstrom.jcc.assembunny.ast;

/**
 * Represents a register in the Assembunny virtual machine.
 * 
 * @author Johan Dykstrom
 */
public enum AssembunnyRegister {

    A,
    B,
    C,
    D;

    /**
     * Returns the Assembunny register that matches the character {@code c}.
     */
    public static AssembunnyRegister from(Character c) {
        return valueOf(c.toString().toUpperCase());
    }
}
