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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

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
public class IfStatement extends AbstractNode implements Statement {

    private final Expression expression;
    private final List<Statement> thenStatements;
    private final List<Statement> elseStatements;

    private IfStatement(int line, int column, Expression expression, List<Statement> thenStatements, List<Statement> elseStatements) {
        super(line, column);
        this.expression = expression;
        this.thenStatements = thenStatements;
        this.elseStatements = elseStatements;
    }

    /**
     * Returns a builder that can be used to build IfStatement objects.
     */
    public static Builder builder(Expression expression, List<Statement> thenStatements) {
        return new Builder(expression, thenStatements);
    }

    /**
     * Returns a builder that can be used to build IfStatement objects.
     */
    public static Builder builder(Expression expression, Statement... thenStatements) {
        return new Builder(expression, thenStatements);
    }

    @Override
    public String toString() {
        return "IF " + expression + " THEN " + formatStatements(thenStatements) +
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
        return new IfStatement(line(), column(), expression, thenStatements, elseStatements);
    }

    /**
     * Returns a copy of this IfStatement with an updated then statements list.
     */
    public IfStatement withThenStatements(List<Statement> thenStatements) {
        return new IfStatement(line(), column(), expression, thenStatements, elseStatements);
    }

    /**
     * Returns a copy of this IfStatement with an updated else statements list.
     */
    public IfStatement withElseStatements(List<Statement> elseStatements) {
        return new IfStatement(line(), column(), expression, thenStatements, elseStatements);
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

    /**
     * An IfStatement builder class.
     */
    public static class Builder {

        private int line;
        private int column;
        private final Expression expression;
        private final List<Statement> thenStatements;
        private List<Statement> elseStatements = Collections.emptyList();

        private Builder(Expression expression, List<Statement> thenStatements) {
            this.expression = expression;
            this.thenStatements = thenStatements;
        }

        private Builder(Expression expression, Statement... thenStatements) {
            this.expression = expression;
            this.thenStatements = asList(thenStatements);
        }

        public Builder line(int line) {
            this.line = line;
            return this;
        }

        public Builder column(int column) {
            this.column = column;
            return this;
        }

        public Builder elseStatements(List<Statement> elseStatements) {
            this.elseStatements = elseStatements;
            return this;
        }

        public Builder elseStatements(Statement... elseStatements) {
            this.elseStatements = asList(elseStatements);
            return this;
        }

        public IfStatement build() {
            return new IfStatement(line, column, expression, thenStatements, elseStatements);
        }
    }
}
