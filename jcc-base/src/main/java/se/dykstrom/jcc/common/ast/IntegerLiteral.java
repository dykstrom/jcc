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

import se.dykstrom.jcc.common.types.I32;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Type;

/**
 * Represents an integer literal such as '17'.
 *
 * @author Johan Dykstrom
 */
public class IntegerLiteral extends AbstractLiteralExpression {

    // I64 literals
    public static final IntegerLiteral ZERO = new IntegerLiteral(0, 0, "0", I64.INSTANCE);
    public static final IntegerLiteral ONE = new IntegerLiteral(0, 0, "1", I64.INSTANCE);

    // I32 literals
    public static final IntegerLiteral ZERO_I32 = new IntegerLiteral(0, 0, "0", I32.INSTANCE);

    public IntegerLiteral(int line, int column, long value) {
        this(line, column, Long.toString(value));
    }

    public IntegerLiteral(int line, int column, String value) {
        super(line, column, value, I64.INSTANCE);
    }

    public IntegerLiteral(int line, int column, String value, Type type) {
        super(line, column, value, type);
    }

    /**
     * Returns a copy of this integer literal, with the value updated.
     */
    public IntegerLiteral withValue(String value) {
        return new IntegerLiteral(line(), column(), value);
    }

    /**
     * Returns the literal value as a long.
     */
    public long asLong() {
        return Long.parseLong(getValue());
    }
}
