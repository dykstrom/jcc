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
 * Represents an assign statement such as 'value := 17' in Tiny or 'LET value% = 17' in Basic.
 * The LHS expression can also be an array element, as in 'LET array%(7) = 17'.
 *
 * @author Johan Dykstrom
 */
public class AssignStatement extends AbstractNode implements Statement {

    private final IdentifierExpression lhsExpression;
    private final Expression rhsExpression;

    public AssignStatement(int line, int column, IdentifierExpression lhsExpression, Expression rhsExpression) {
        super(line, column);
        this.lhsExpression = lhsExpression;
        this.rhsExpression = rhsExpression;
    }

    @Override
    public String toString() {
        return lhsExpression + " = " + rhsExpression;
    }

    public IdentifierExpression getLhsExpression() { return lhsExpression; }

    public Expression getRhsExpression() {
        return rhsExpression;
    }

    /**
     * Returns a copy of this assign statement, with the LHS expression set to {@code lhsExpression}.
     */
    public AssignStatement withLhsExpression(IdentifierExpression lhsExpression) {
        return new AssignStatement(line(), column(), lhsExpression, rhsExpression);
    }

    /**
     * Returns a copy of this assign statement, with the RHS expression set to {@code rhsExpression}.
     */
    public AssignStatement withRhsExpression(Expression rhsExpression) {
        return new AssignStatement(line(), column(), lhsExpression, rhsExpression);
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
