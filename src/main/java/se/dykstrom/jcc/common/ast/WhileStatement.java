/*
 * Copyright (C) 2017 Johan Dykstrom
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

import static java.util.stream.Collectors.joining;
import static se.dykstrom.jcc.common.utils.FormatUtils.formatLineNumber;

import java.util.List;
import java.util.Objects;

/**
 * Represents a WHILE statement such as:
 * 
 * WHILE x > 0
 *   PRINT x
 *   x = x - 1
 * WEND
 *
 * @author Johan Dykstrom
 */
public class WhileStatement extends Statement {

    private final Expression expression;
    private final List<Statement> statements;

    public WhileStatement(int line, int column, Expression expression, List<Statement> statements) {
        this(line, column, expression, statements, null);
    }

    public WhileStatement(int line, int column, Expression expression, List<Statement> statements, String label) {
        super(line, column, label);
        this.expression = expression;
        this.statements = statements;
    }

    @Override
    public String toString() {
        return formatLineNumber(getLabel()) +  "WHILE " + expression + " " + formatStatements(statements);
    }

    private String formatStatements(List<Statement> statements) {
        return statements.stream().map(Statement::toString).collect(joining(" : "));
    }

    public Expression getExpression() {
        return expression;
    }

    public List<Statement> getStatements() {
        return statements;
    }

    /**
     * Returns a copy of this WhileStatement with an updated expression.
     */
    public WhileStatement withExpression(Expression expression) {
        return new WhileStatement(getLine(), getColumn(), expression, statements, getLabel());
    }

    /**
     * Returns a copy of this WhileStatement with an updated statements list.
     */
    public WhileStatement withStatements(List<Statement> statements) {
        return new WhileStatement(getLine(), getColumn(), expression, statements, getLabel());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WhileStatement that = (WhileStatement) o;
        return Objects.equals(this.expression, that.expression) && Objects.equals(this.statements, that.statements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, statements);
    }
}
