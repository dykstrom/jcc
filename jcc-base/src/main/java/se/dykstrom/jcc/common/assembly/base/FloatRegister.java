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

package se.dykstrom.jcc.common.assembly.base;

/**
 * Enumerates all available floating point registers.
 *
 * @author Johan Dykstrom
 */
public enum FloatRegister {

    XMM0(true),
    XMM1(true),
    XMM2(true),
    XMM3(true),
    XMM4(true),
    XMM5(true),
    XMM6(false),
    XMM7(false),
    XMM8(false),
    XMM9(false),
    XMM10(false),
    XMM11(false),
    XMM12(false),
    XMM13(false),
    XMM14(false),
    XMM15(false);

    private final boolean isVolatile;

    FloatRegister(boolean isVolatile) {
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
