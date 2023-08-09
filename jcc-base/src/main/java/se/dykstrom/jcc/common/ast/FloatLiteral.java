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

package se.dykstrom.jcc.common.ast;

import se.dykstrom.jcc.common.types.F64;

/**
 * Represents a floating point literal such as '3.14' or '1.234e-10'.
 *
 * @author Johan Dykstrom
 */
public class FloatLiteral extends AbstractLiteralExpression {

    public FloatLiteral(int line, int column, double value) {
        this(line, column, Double.toString(value));
    }

    public FloatLiteral(int line, int column, String value) {
        super(line, column, value, F64.INSTANCE);
    }

    /**
     * Returns a copy of this float literal, with the value updated.
     */
    public FloatLiteral withValue(String value) {
        return new FloatLiteral(line(), column(), value);
    }

    /**
     * Returns the literal value as a double.
     */
    public double asDouble() {
        return Double.parseDouble(getValue());
    }
}
