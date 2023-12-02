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
 * Represents the void type, that is, no type.
 *
 * @author Johan Dykstrom
 */
public class Void extends AbstractType {

    public static final Void INSTANCE = new Void();

    @Override
    public String getDefaultValue() {
        throw new UnsupportedOperationException("void");
    }

    @Override
    public String getFormat() {
        throw new UnsupportedOperationException("void");
    }
}
