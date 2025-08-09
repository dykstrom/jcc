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

import se.dykstrom.jcc.common.functions.Function;

import java.util.Objects;

/**
 * Represents a call to the abs function.
 *
 * @author Johan Dykstrom
 */
public class AbsExpression extends UnaryExpression {

    private final Function function;

    public AbsExpression(final Expression expression, final Function function) {
        super(0, 0, expression);
        this.function = function;
    }

    public Function function() {
        return function;
    }

    @Override
    public String toString() {
        return "abs(" + getExpression() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AbsExpression that = (AbsExpression) o;
        return Objects.equals(function, that.function);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), function);
    }
}
