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

package se.dykstrom.jcc.common.error;

/**
 * Exception thrown when an invalid value is found.
 *
 * @author Johan Dykstrom
 */
public class InvalidValueException extends SemanticsException {

    private final String value;

    public InvalidValueException(final String msg, final String value) {
        super(msg);
        this.value = value;
    }

    /**
     * Returns the invalid value.
     */
    public String value() {
        return value;
    }
}
