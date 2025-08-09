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

package se.dykstrom.jcc.basic.ast.statement;

import se.dykstrom.jcc.common.ast.AbstractNode;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.Statement;

import java.util.Objects;

/**
 * Represents a "SLEEP" statement such as "SLEEP 5".
 *
 * @author Johan Dykstrom
 */
public class SleepStatement extends AbstractNode implements Statement {

    private final Expression expression;

    public SleepStatement(int line, int column, Expression expression) {
        super(line, column);
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    /**
     * Returns a new SleepExpression, based on this, with the expression updated.
     */
    public SleepStatement withExpression(final Expression expression) {
        return new SleepStatement(line(), column(), expression);
    }

    @Override
    public String toString() {
        return "SLEEP" + ((expression == null) ? "" : " " + expression);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SleepStatement that = (SleepStatement) o;
        return Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression);
    }
}
