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

package se.dykstrom.jcc.common.types;

/**
 * Represents the 64-bit signed integer type.
 *
 * @author Johan Dykstrom
 */
public class I64 extends AbstractType implements NumericType {

    public static final I64 INSTANCE = new I64();

    @Override
    public String llvmName() {
        return "i64";
    }

    @Override
    public String getDefaultValue() {
        return "0";
    }

    @Override
    public String getFormat() {
        return "%lld";
    }
}
