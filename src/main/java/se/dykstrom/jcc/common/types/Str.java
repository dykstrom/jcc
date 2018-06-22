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
 * Represents the string type.
 *
 * TODO: The default value used in this class is valid for BASIC. Other languages have other default values.
 *
 * @author Johan Dykstrom
 */
public class Str extends AbstractType {

    public static final Str INSTANCE = new Str();

    /** The empty string. */
    public static final String EMPTY_STRING_VALUE = "\"\",0";

    /** The default value of a string variable is a reference to this string constant. */
    public static final String EMPTY_STRING_NAME = "_empty";

    /** Mapped name to use in code generation. */
    private static final String EMPTY_STRING_MAPPED_NAME = "_" + EMPTY_STRING_NAME;

    @Override
    public String getDefaultValue() {
        return EMPTY_STRING_MAPPED_NAME;
    }

    @Override
    public String getFormat() {
        return "%s";
    }
}
