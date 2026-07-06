/*
 * Copyright (C) 2026 Johan Dykstrom
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

package se.dykstrom.jcc.basic.ast.expression;

import se.dykstrom.jcc.common.ast.AbstractNode;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.IdentifierExpression;
import se.dykstrom.jcc.common.ast.TypedExpression;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Type;

/**
 * Represents a call to the intrinsic function 'ubound'. The dimension is 1-based and always
 * present; a call without a dimension argument defaults to dimension 1.
 *
 * @author Johan Dykstrom
 */
public class UboundExpression extends AbstractNode implements TypedExpression {

    private final IdentifierExpression array;
    private final Expression dimension;

    public UboundExpression(final IdentifierExpression array, final Expression dimension) {
        super(0, 0);
        this.array = array;
        this.dimension = dimension;
    }

    public IdentifierExpression array() {
        return array;
    }

    public Expression dimension() {
        return dimension;
    }

    @Override
    public String toString() {
        return "ubound(" + array + ", " + dimension + ")";
    }

    @Override
    public Type type() {
        return I64.INSTANCE;
    }
}
