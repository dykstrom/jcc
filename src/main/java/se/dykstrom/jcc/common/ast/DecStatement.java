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

/**
 * Represents a decrement statement such as 'value = value - 1' in Basic. In C it would be 'value--'.
 *
 * @author Johan Dykstrom
 */
public class DecStatement extends AbstractNode implements Statement {

    private final IdentifierExpression lhsExpression;

    public DecStatement(int line, int column, IdentifierExpression lhsExpression) {
        super(line, column);
        this.lhsExpression = lhsExpression;
    }

    @Override
    public String toString() {
        return lhsExpression + "--";
    }

    public IdentifierExpression getLhsExpression() {
        return lhsExpression;
    }

    /**
     * Creates a decrement statement from an assignment statement.
     */
    public static DecStatement from(AssignStatement statement) {
        return new DecStatement(statement.line(), statement.column(), statement.getLhsExpression());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DecStatement that = (DecStatement) o;
        return Objects.equals(lhsExpression, that.lhsExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lhsExpression);
    }
}
