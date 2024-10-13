/*
 * Copyright (C) 2024 Johan Dykstrom
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

import se.dykstrom.jcc.common.types.Type;

/**
 * Truncates an integer value to a smaller integer type.
 *
 * @author Johan Dykstrom
 */
public class TruncateExpression extends UnaryExpression implements TypedExpression {

    private final Type destinationType;

    public TruncateExpression(final int line, final int column, final Expression source, final Type destinationType) {
        super(line, column, source);
        this.destinationType = destinationType;
    }

    @Override
    public String toString() {
        return destinationType.getName() + "(" + getExpression() + ")";
    }

    @Override
    public Type getType() {
        return destinationType;
    }
}
