/*
 * Copyright (C) 2023 Johan Dykstrom
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

package se.dykstrom.jcc.col.ast.statement;

import se.dykstrom.jcc.common.ast.AbstractNode;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.Statement;

import java.util.Objects;

/**
 * Represents a println statement such as 'println "Hello, world!"'.
 *
 * @author Johan Dykstrom
 */
public class PrintlnStatement extends AbstractNode implements Statement {

    private final Expression expression;

    public PrintlnStatement(final int line, final int column, final Expression expression) {
        super(line, column);
        this.expression = expression;
    }

    public PrintlnStatement(final Expression expression) {
        this(0, 0, expression);
    }

    @Override
    public String toString() {
        return "println" + toString(expression);
    }

    private String toString(final Expression expression) {
        return expression != null ? " " + expression : "";
    }

    public Expression expression() {
        return expression;
    }

    /**
     * Returns a copy of this statement, with the expression set to {@code expression}.
     */
    public PrintlnStatement withExpression(final Expression expression) {
        return new PrintlnStatement(line(), column(), expression);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PrintlnStatement that = (PrintlnStatement) o;
        return Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression);
    }
}
