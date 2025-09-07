/*
 * Copyright (C) 2025 Johan Dykstrom
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

package se.dykstrom.jcc.common.types;

/**
 * Represents the 8-bit signed integer type.
 *
 * @author Johan Dykstrom
 */
public class I8 extends AbstractType implements IntegerType {

    public static final I8 INSTANCE = new I8();

    @Override
    public String llvmName() {
        return "i8";
    }

    @Override
    public String llvmDefaultValue() {
        return "0";
    }

    @Override
    public String getDefaultValue() {
        return "0";
    }

    @Override
    public String getFormat() {
        return "%d";
    }

    @Override
    public int bits() {
        return 8;
    }
}
