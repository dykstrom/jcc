/*
 * Copyright (C) 2022 Johan Dykstrom
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
 * Represents a statement with a label.
 *
 * @author Johan Dykstrom
 */
public class LabelledStatement extends AbstractNode implements Statement {

    private final String label;
    private final Statement statement;

    public LabelledStatement(final String label, final Statement statement) {
        super(statement.line(), statement.column());
        this.statement = requireNonNull(statement);
        this.label = requireNonNull(label);
    }

    public String label() {
        return label;
    }

    public Statement statement() {
        return statement;
    }

    public Statement withStatement(final Statement statement) {
        return new LabelledStatement(label, statement);
    }

    @Override
    public String toString() {
        return toString(label) + statement;
    }

    private static String toString(final String label) {
        if (isNumber(label)) {
            return label + " ";
        } else {
            return label + ": ";
        }
    }

    private static boolean isNumber(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabelledStatement that = (LabelledStatement) o;
        return label.equals(that.label) && statement.equals(that.statement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, statement);
    }
}
