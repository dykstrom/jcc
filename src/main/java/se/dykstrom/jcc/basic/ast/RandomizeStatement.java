/*
 * Copyright (C) 2019 Johan Dykstrom
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

package se.dykstrom.jcc.basic.ast;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.Statement;

import java.util.Objects;

import static se.dykstrom.jcc.common.utils.FormatUtils.formatLineNumber;

/**
 * Represents a "RANDOMIZE" statement such as '10 RANDOMIZE 99'.
 *
 * @author Johan Dykstrom
 */
public class RandomizeStatement extends Statement {

    private final Expression expression;

    public RandomizeStatement(int line, int column) {
        this(line, column, null, null);
    }

    public RandomizeStatement(int line, int column, String label) {
        this(line, column, null, label);
    }

    public RandomizeStatement(int line, int column, Expression expression) {
        this(line, column, expression, null);
    }

    public RandomizeStatement(int line, int column, Expression expression, String label) {
        super(line, column, label);
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    /**
     * Returns a new RandomizeExpression, based on this, with the expression updated.
     */
    public RandomizeStatement withExpression(Expression expression) {
        return new RandomizeStatement(line(), column(), expression, label());
    }

    @Override
    public String toString() {
        return formatLineNumber(label()) +  "RANDOMIZE" + ((expression == null) ? "" : " " + expression);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RandomizeStatement that = (RandomizeStatement) o;
        return Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression);
    }
}
