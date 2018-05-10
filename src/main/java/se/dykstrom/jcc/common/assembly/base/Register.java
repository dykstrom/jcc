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

package se.dykstrom.jcc.common.assembly.base;

/**
 * Enumerates all available general purpose registers.
 *
 * @author Johan Dykstrom
 */
public enum Register {

    RAX(true),
    RBX(false),
    RCX(true),
    RDX(true),
    RBP(false),
    RDI(false),
    RSI(false),
    RSP(false),
    R8(true),
    R9(true),
    R10(true),
    R11(true),
    R12(false),
    R13(false),
    R14(false),
    R15(false);

    private final boolean isVolatile;

    Register(boolean isVolatile) {
        this.isVolatile = isVolatile;
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }

    /**
     * Returns {@code true} if this register is volatile.
     */
    public boolean isVolatile() {
        return isVolatile;
    }
}
