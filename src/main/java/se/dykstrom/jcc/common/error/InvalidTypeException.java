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

package se.dykstrom.jcc.common.error;

import se.dykstrom.jcc.common.types.Type;

/**
 * Exception thrown when an invalid type is found.
 *
 * @author Johan Dykstrom
 */
public class InvalidTypeException extends SemanticsException {

    private final Type type;

    public InvalidTypeException(String msg, Type type) {
        super(msg);
        this.type = type;
    }

    /**
     * Returns the invalid type.
     */
    public Type getType() {
        return type;
    }
}
