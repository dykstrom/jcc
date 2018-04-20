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

package se.dykstrom.jcc.common.ast;

import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Type;

import java.util.Objects;

/**
 * Represents an integer literal such as '17'.
 *
 * @author Johan Dykstrom
 */
public class IntegerLiteral extends LiteralExpression {

    private final String value;

    public IntegerLiteral(int line, int column, String value) {
        super(line, column);
        this.value = value;
    }

    /**
     * Returns a copy of this integer literal, with the value updated.
     */
    public IntegerLiteral withValue(String value) {
        return new IntegerLiteral(getLine(), getColumn(), value);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public Type getType() {
        return I64.INSTANCE;
    }

    /**
     * Returns the value as a string.
     */
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntegerLiteral that = (IntegerLiteral) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
