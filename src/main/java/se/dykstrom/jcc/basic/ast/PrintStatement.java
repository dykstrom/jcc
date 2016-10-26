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

package se.dykstrom.jcc.basic.ast;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.Statement;

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.joining;

/**
 * Represents a print statement such as '10 PRINT "Hello, world!"'.
 *
 * @author Johan Dykstrom
 */
public class PrintStatement extends Statement {

    private final List<Expression> expressions;

    public PrintStatement(int line, int column, List<Expression> expressions) {
        this(line, column, expressions, null);
    }

    public PrintStatement(int line, int column, List<Expression> expressions, String label) {
        super(line, column, label);
        this.expressions = expressions;
    }

    @Override
    public String toString() {
        return formatLineNumber(getLabel()) + " PRINT " + toString(expressions);
    }

    private String formatLineNumber(String lineNumber) {
        return (lineNumber != null) ? lineNumber : "<line>";
    }

    private String toString(List<Expression> expressions) {
        return expressions.stream().map(Expression::toString).collect(joining(", "));
    }

    /**
     * Returns the list of expressions.
     */
    public List<Expression> getExpressions() {
        return expressions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrintStatement that = (PrintStatement) o;
        return Objects.equals(expressions, that.expressions) && Objects.equals(getLabel(), that.getLabel());
    }

    @Override
    public int hashCode() {
        return Objects.hash(expressions, getLabel());
    }
}
