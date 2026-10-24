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
import se.dykstrom.jcc.common.ast.TypedExpression;
import se.dykstrom.jcc.common.types.I64;
import se.dykstrom.jcc.common.types.Type;

/**
 * Represents a call to the intrinsic function 'sgn'.
 * <p>
 * The subexpression is a node rather than a pair of comparisons built in the inline
 * map, so that its code generator can evaluate the argument once.
 *
 * @author Johan Dykstrom
 */
public class SgnExpression extends AbstractNode implements TypedExpression {

    private final Expression expression;

    public SgnExpression(final Expression expression) {
        super(0, 0);
        this.expression = expression;
    }

    public Expression expression() {
        return expression;
    }

    @Override
    public String toString() {
        return "sgn(" + expression.toString() + ")";
    }

    @Override
    public Type type() {
        return I64.INSTANCE;
    }
}
