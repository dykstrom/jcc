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

package se.dykstrom.jcc.common.ast;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Represents a multiply-assign statement such as 'value = value * 5' in BASIC, or 'value *= 5' in C.
 *
 * @author Johan Dykstrom
 */
public class MulAssignStatement extends AbstractNode implements Statement {

    private final IdentifierExpression lhsExpression;
    private final LiteralExpression rhsExpression;

    public MulAssignStatement(final int line,
                              final int column,
                              final IdentifierExpression lhsExpression,
                              final LiteralExpression rhsExpression) {
        super(line, column);
        this.lhsExpression = requireNonNull(lhsExpression);
        this.rhsExpression = requireNonNull(rhsExpression);
    }

    @Override
    public String toString() {
        return lhsExpression + " *= " + rhsExpression;
    }

    public IdentifierExpression lhsExpression() { return lhsExpression; }

    public LiteralExpression rhsExpression() {
        return rhsExpression;
    }

    /**
     * Creates a multiply-assign statement from a normal assignment statement.
     */
    public static MulAssignStatement from(final AssignStatement statement, final LiteralExpression rhsExpression) {
        return new MulAssignStatement(statement.line(), statement.column(), statement.getLhsExpression(), rhsExpression);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MulAssignStatement that = (MulAssignStatement) o;
        return Objects.equals(lhsExpression, that.lhsExpression) && Objects.equals(rhsExpression, that.rhsExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lhsExpression, rhsExpression);
    }
}
