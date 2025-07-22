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

import se.dykstrom.jcc.common.types.I64;

/**
 * Represents a cast to type i64.
 *
 * @author Johan Dykstrom
 */
public class CastToI64Expression extends CastToIntExpression {

    public CastToI64Expression(final int line, final int column, final Expression expression) {
        super(line, column, expression, I64.INSTANCE);
    }

    public CastToI64Expression(final Expression expression) {
        this(0, 0, expression);
    }
}
