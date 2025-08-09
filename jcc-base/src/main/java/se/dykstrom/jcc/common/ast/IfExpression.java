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

package se.dykstrom.jcc.common.ast;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Represents an if expression, such as 'if x < 0 then 10 else 20'.
 *
 * @author Johan Dykstrom
 */
public class IfExpression extends AbstractNode implements Expression {

    private final Expression ifExpr;
    private final Expression thenExpr;
    private final Expression elseExpr;

    public IfExpression(final int line,
                        final int column,
                        final Expression ifExpr,
                        final Expression thenExpr,
                        final Expression elseExpr) {
        super(line, column);
        this.ifExpr = requireNonNull(ifExpr);
        this.thenExpr = requireNonNull(thenExpr);
        this.elseExpr = requireNonNull(elseExpr);
    }

    public IfExpression(final Expression ifExpr,
                        final Expression thenExpr,
                        final Expression elseExpr) {
        this(0, 0, ifExpr, thenExpr, elseExpr);
    }

    @Override
    public String toString() {
        return "if " + ifExpr + " then " + thenExpr + " else " + elseExpr;
    }

    public Expression elseExpr() {
        return elseExpr;
    }

    public IfExpression withElseExpr(final Expression elseExpr) {
        return new IfExpression(line(), column(), ifExpr, thenExpr, elseExpr);
    }

    public Expression ifExpr() {
        return ifExpr;
    }

    public IfExpression withIfExpr(final Expression ifExpr) {
        return new IfExpression(line(), column(), ifExpr, thenExpr, elseExpr);
    }

    public Expression thenExpr() {
        return thenExpr;
    }

    public IfExpression withThenExpr(final Expression thenExpr) {
        return new IfExpression(line(), column(), ifExpr, thenExpr, elseExpr);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        IfExpression that = (IfExpression) o;
        return Objects.equals(ifExpr, that.ifExpr) && Objects.equals(thenExpr, that.thenExpr) && Objects.equals(elseExpr, that.elseExpr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ifExpr, thenExpr, elseExpr);
    }
}
