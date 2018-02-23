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

    XMM0(),
    XMM1(),
    XMM2(),
    XMM3(),
    XMM4(),
    XMM5(),
    XMM6(),
    XMM7(),
    XMM8(),
    XMM9(),
    XMM10(),
    XMM11(),
    XMM12(),
    XMM13(),
    XMM14(),
    XMM15();

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
