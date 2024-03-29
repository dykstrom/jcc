/*
 * Copyright (C) 2016 Johan Dykstrom
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

/**
 * Represents an exit statement.
 *
 * @author Johan Dykstrom
 */
public class ExitStatement extends AbstractNode implements Statement {

    private final Expression expression;

    public ExitStatement(int line, int column, Expression expression) {
        super(line, column);
        this.expression = expression;
    }

    /**
     * Returns the exit status expression.
     */
    public Expression getExpression() {
        return expression;
    }

    @Override
    public String toString() {
        return "exit(" + expression + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExitStatement that = (ExitStatement) o;
        return Objects.equals(this.expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression);
    }
}
