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

import se.dykstrom.jcc.common.symbols.Identifier;

import java.util.Objects;

import static se.dykstrom.jcc.common.utils.FormatUtils.formatLineNumber;

/**
 * Represents an assign statement such as 'value := 17' in Tiny or 'LET value = 17' in Basic.
 *
 * @author Johan Dykstrom
 */
public class AssignStatement extends Statement {

    private final Identifier identifier;
    private final Expression expression;

    public AssignStatement(int line, int column, Identifier identifier, Expression expression) {
        super(line, column);
        this.identifier = identifier;
        this.expression = expression;
    }

    @Override
    public String toString() {
        return formatLineNumber(getLabel()) +  " " + identifier.getName() + " : " + identifier.getType().getName()
                + " = " + expression;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public Expression getExpression() {
        return expression;
    }

    /**
     * Returns a copy of this assign statement, with the identifier set to {@code identifier}.
     */
    public AssignStatement withIdentifier(Identifier identifier) {
        return new AssignStatement(getLine(), getColumn(), identifier, expression);
    }

    /**
     * Returns a copy of this assign statement, with the expression set to {@code expression}.
     */
    public AssignStatement withExpression(Expression expression) {
        return new AssignStatement(getLine(), getColumn(), identifier, expression);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssignStatement that = (AssignStatement) o;
        return Objects.equals(identifier, that.identifier) && Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, expression);
    }
}
