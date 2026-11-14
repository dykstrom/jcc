/*
 * Copyright (C) 2026 Johan Dykstrom
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
 * Represents the 128-bit signed integer type.
 *
 * @author Johan Dykstrom
 */
public class I128 extends AbstractType implements IntegerType {

    public static final I128 INSTANCE = new I128();

    @Override
    public String llvmName() {
        return "i128";
    }

    @Override
    public String llvmDefaultValue() {
        return "0";
    }

    @Override
    public String getFormat() {
        // No printf conversion exists for 128 bits
        throw new UnsupportedOperationException("i128 has no printf format, use col_string_i128");
    }

    @Override
    public int bits() {
        return 128;
    }
}
