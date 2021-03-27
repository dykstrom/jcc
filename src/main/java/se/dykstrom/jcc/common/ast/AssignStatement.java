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

import static se.dykstrom.jcc.common.utils.FormatUtils.formatLineNumber;

/**
 * Represents an assign statement such as 'value := 17' in Tiny or 'LET value% = 17' in Basic.
 * Also possible is 'LET array%(7) = 17'.
 *
 * @author Johan Dykstrom
 */
public class AssignStatement extends Statement {

    private final AssignableExpression lhsExpression;
    private final Expression rhsExpression;

    public AssignStatement(int line, int column, Expression lhsExpression, Expression rhsExpression) {
        this(line, column, lhsExpression, rhsExpression, null);
    }

    public AssignStatement(int line, int column, Expression lhsExpression, Expression rhsExpression, String label) {
        super(line, column, label);
        // We need this strange cast because Expression is not an interface and AssignableExpression cannot inherit it.
        this.lhsExpression = (AssignableExpression) lhsExpression;
        this.rhsExpression = rhsExpression;
    }

    @Override
    public String toString() {
        return formatLineNumber(getLabel()) +  lhsExpression + " = " + rhsExpression;
    }

    public Expression getLhsExpression() { return (Expression) lhsExpression; }

    public Expression getRhsExpression() {
        return rhsExpression;
    }

    /**
     * Returns a copy of this assign statement, with the LHS expression set to {@code lhsExpression}.
     */
    public AssignStatement withLhsExpression(Expression lhsExpression) {
        return new AssignStatement(getLine(), getColumn(), lhsExpression, rhsExpression, getLabel());
    }

    /**
     * Returns a copy of this assign statement, with the RHS expression set to {@code rhsExpression}.
     */
    public AssignStatement withRhsExpression(Expression rhsExpression) {
        return new AssignStatement(getLine(), getColumn(), (Expression) lhsExpression, rhsExpression, getLabel());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssignStatement that = (AssignStatement) o;
        return Objects.equals(lhsExpression, that.lhsExpression) && Objects.equals(rhsExpression, that.rhsExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lhsExpression, rhsExpression);
    }
}
