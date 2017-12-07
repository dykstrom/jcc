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

package se.dykstrom.jcc.assembunny.ast;

import java.util.Objects;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.Statement;

/**
 * Represents a "output with newline" statement such as 'outn a'.
 *
 * @author Johan Dykstrom
 */
public class OutnStatement extends Statement {

    private final Expression expression;

    public OutnStatement(int line, int column, Expression expression) {
        this(line, column, expression, null);
    }

    public OutnStatement(int line, int column, Expression expression, String label) {
        super(line, column, label);
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "outn " + expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutnStatement that = (OutnStatement) o;
        return Objects.equals(this.expression, that.expression) && Objects.equals(this.getLabel(), that.getLabel());
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, getLabel());
    }
}
