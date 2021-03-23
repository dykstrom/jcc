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

import se.dykstrom.jcc.common.types.Identifier;

import java.util.Objects;

import static se.dykstrom.jcc.common.utils.FormatUtils.formatLineNumber;

/**
 * Represents an add-assign statement such as 'value = value + 5' in Basic, or 'value += 5' in C.
 *
 * @author Johan Dykstrom
 */
public class AddAssignStatement extends Statement {

    private final Identifier identifier;
    private final LiteralExpression expression;

    public AddAssignStatement(int line, int column, Identifier identifier, LiteralExpression expression) {
        this(line, column, identifier, expression, null);
    }

    public AddAssignStatement(int line, int column, Identifier identifier, LiteralExpression expression, String label) {
        super(line, column, label);
        this.identifier = identifier;
        this.expression = expression;
    }

    @Override
    public String toString() {
        return formatLineNumber(getLabel()) +  identifier.getName() + " : " + identifier.getType().getName()
                + " += " + expression;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public LiteralExpression getExpression() {
        return expression;
    }

    /**
     * Creates an add-assign statement from a normal assignment statement.
     */
    public static AddAssignStatement from(AssignStatement statement, LiteralExpression expression) {
        return new AddAssignStatement(statement.getLine(), statement.getColumn(), statement.getIdentifier(), expression, statement.getLabel());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AddAssignStatement that = (AddAssignStatement) o;
        return Objects.equals(identifier, that.identifier) && Objects.equals(expression, that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, expression);
    }
}
