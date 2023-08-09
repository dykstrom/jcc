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

package se.dykstrom.jcc.tiny.ast;

import se.dykstrom.jcc.common.ast.AbstractNode;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.Statement;

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.joining;

/**
 * Represents a write statement such as 'WRITE value'.
 *
 * @author Johan Dykstrom
 */
public class WriteStatement extends AbstractNode implements Statement {

    private final List<Expression> expressions;

    public WriteStatement(int line, int column, List<Expression> expressions) {
        super(line, column);
        this.expressions = expressions;
    }

    @Override
    public String toString() {
        return "WRITE " + toString(expressions);
    }

    private String toString(List<Expression> expressions) {
        return expressions.stream().map(Expression::toString).collect(joining(", "));
    }

    public List<Expression> getExpressions() {
        return expressions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WriteStatement that = (WriteStatement) o;
        return Objects.equals(expressions, that.expressions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expressions);
    }
}
