/*
 * Copyright (C) 2025 Johan Dykstrom
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

package se.dykstrom.jcc.col.ast.expression;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.TypedExpression;
import se.dykstrom.jcc.common.ast.UnaryExpression;
import se.dykstrom.jcc.common.types.I32;
import se.dykstrom.jcc.common.types.Type;

/**
 * Represents a function call to the intrinsic function "println".
 *
 * @author Johan Dykstrom
 */
public class PrintlnExpression extends UnaryExpression implements TypedExpression {

    public PrintlnExpression(final int line, final int column, final Expression expression) {
        super(line, column, expression);
    }

    public PrintlnExpression(final Expression expression) {
        this(0, 0, expression);
    }

    /**
     * The intrinsic "println" always returns an integer, regardless of the type of its argument.
     */
    @Override
    public Type type() {
        return I32.INSTANCE;
    }

    @Override
    public String toString() {
        return "println(" + getExpression() + ")";
    }
}
