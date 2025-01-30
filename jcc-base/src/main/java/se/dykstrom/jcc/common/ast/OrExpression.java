/*
 * Copyright (C) 2017 Johan Dykstrom
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
 * Represents a bitwise OR expression.
 *
 * @author Johan Dykstrom
 */
public class OrExpression extends BinaryExpression implements BitwiseExpression {

    public OrExpression(final int line, final int column, final Expression left, final Expression right) {
        super(line, column, left, right);
    }

    public OrExpression(final Expression left, final Expression right) {
        this(0, 0, left, right);
    }

    @Override
    public String toString() {
        return getLeft() + " OR " + getRight();
    }
}
