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

package se.dykstrom.jcc.common.ast;

import java.util.Objects;

import static se.dykstrom.jcc.common.utils.FormatUtils.formatLineNumber;

/**
 * Represents a sub-assign statement such as 'value = value - 5' in Basic, or 'value -= 5' in C.
 *
 * @author Johan Dykstrom
 */
public class SubAssignStatement extends Statement {

    private final IdentifierExpression lhsExpression;
    private final LiteralExpression rhsExpression;

    public SubAssignStatement(int line, int column, IdentifierExpression lhsExpression, LiteralExpression rhsExpression) {
        this(line, column, lhsExpression, rhsExpression, null);
    }

    public SubAssignStatement(int line, int column, IdentifierExpression lhsExpression, LiteralExpression rhsExpression, String label) {
        super(line, column, label);
        this.lhsExpression = lhsExpression;
        this.rhsExpression = rhsExpression;
    }

    @Override
    public String toString() {
        return formatLineNumber(label()) + lhsExpression + " -= " + rhsExpression;
    }

    public IdentifierExpression getLhsExpression() {
        return lhsExpression;
    }

    public LiteralExpression getRhsExpression() {
        return rhsExpression;
    }

    /**
     * Creates a sub-assign statement from a normal assignment statement.
     */
    public static SubAssignStatement from(AssignStatement statement, LiteralExpression rhsExpression) {
        return new SubAssignStatement(statement.line(), statement.column(), statement.getLhsExpression(), rhsExpression, statement.label());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubAssignStatement that = (SubAssignStatement) o;
        return Objects.equals(lhsExpression, that.lhsExpression) && Objects.equals(rhsExpression, that.rhsExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lhsExpression, rhsExpression);
    }
}
