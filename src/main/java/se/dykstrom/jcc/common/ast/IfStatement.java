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

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static se.dykstrom.jcc.common.utils.FormatUtils.formatLineNumber;

import java.util.List;
import java.util.Objects;

/**
 * Represents an IF statement such as 'if x > 0 then goto 10 else goto 20'. The ELSE part is optional.
 * 
 * Another possibility is:
 * 
 * IF x = 1 THEN
 *   PRINT 1
 * ELSEIF x = 2 THEN
 *   PRINT 2
 * ELSE
 *   PRINT 3
 * ENDIF
 *
 * @author Johan Dykstrom
 */
public class IfStatement extends Statement {

    private final Expression expression;
    private final List<Statement> thenStatements;
    private final List<Statement> elseStatements;

    public IfStatement(int line, int column, Expression expression, List<Statement> thenStatements) {
        this(line, column, expression, thenStatements, emptyList(), null);
    }

    public IfStatement(int line, int column, Expression expression, List<Statement> thenStatements, String label) {
        this(line, column, expression, thenStatements, emptyList(), label);
    }

    public IfStatement(int line, int column, Expression expression, List<Statement> thenStatements, List<Statement> elseStatements) {
        this(line, column, expression, thenStatements, elseStatements, null);
    }

    public IfStatement(int line, int column, Expression expression, List<Statement> thenStatements, List<Statement> elseStatements, String label) {
        super(line, column, label);
        this.expression = expression;
        this.thenStatements = thenStatements;
        this.elseStatements = elseStatements;
    }

    @Override
    public String toString() {
        return formatLineNumber(getLabel()) +  "IF " + expression + " THEN " + formatStatements(thenStatements) +
                (elseStatements.isEmpty() ? "" : " ELSE " + formatStatements(elseStatements));
    }

    private String formatStatements(List<Statement> statements) {
        return statements.stream().map(Statement::toString).collect(joining(" : "));
    }

    public Expression getExpression() {
        return expression;
    }

    public List<Statement> getThenStatements() {
        return thenStatements;
    }

    public List<Statement> getElseStatements() {
        return elseStatements;
    }

    /**
     * Returns a copy of this IfStatement with an updated expression.
     */
    public IfStatement withExpression(Expression expression) {
        return new IfStatement(getLine(), getColumn(), expression, thenStatements, elseStatements, getLabel());
    }

    /**
     * Returns a copy of this IfStatement with an updated then statements list.
     */
    public IfStatement withThenStatements(List<Statement> thenStatements) {
        return new IfStatement(getLine(), getColumn(), expression, thenStatements, elseStatements, getLabel());
    }

    /**
     * Returns a copy of this IfStatement with an updated else statements list.
     */
    public IfStatement withElseStatements(List<Statement> elseStatements) {
        return new IfStatement(getLine(), getColumn(), expression, thenStatements, elseStatements, getLabel());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IfStatement that = (IfStatement) o;
        return Objects.equals(this.expression, that.expression) && 
               Objects.equals(this.thenStatements, that.thenStatements) && 
               Objects.equals(this.elseStatements, that.elseStatements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, thenStatements, elseStatements);
    }
}
