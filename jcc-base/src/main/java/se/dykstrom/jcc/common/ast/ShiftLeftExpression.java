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

package se.dykstrom.jcc.common.ast;

/**
 * Represents a shift left operation, like 'value << 2' in C.
 *
 * @author Johan Dykstrom
 */
public class ShiftLeftExpression extends BinaryExpression {

    public ShiftLeftExpression(int line, int column, Expression expression, Expression shift) {
        super(line, column, expression, shift);
    }

    @Override
    public String toString() {
        return getLeft() + " << " + getRight();
    }
}
