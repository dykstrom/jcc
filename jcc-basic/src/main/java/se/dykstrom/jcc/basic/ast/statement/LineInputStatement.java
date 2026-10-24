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

package se.dykstrom.jcc.basic.ast.statement;

import se.dykstrom.jcc.common.ast.AbstractNode;
import se.dykstrom.jcc.common.ast.IdentifierNameExpression;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.types.Identifier;

import java.util.Objects;

/**
 * Represents an "LINE INPUT" statement such as '10 LINE INPUT "Enter first name:"; name$'.
 *
 * @author Johan Dykstrom
 */
public class LineInputStatement extends AbstractNode implements Statement {

    private final boolean inhibitNewline;
    private final String prompt;
    private final IdentifierNameExpression identifierExpression;

    private LineInputStatement(int line,
                               int column,
                               boolean inhibitNewline,
                               String prompt,
                               IdentifierNameExpression identifierExpression) {
        super(line, column);
        this.inhibitNewline = inhibitNewline;
        this.prompt = prompt;
        this.identifierExpression = identifierExpression;
    }

    /**
     * Returns a builder that can be used to build {@code LineInputStatement} objects.
     */
    public static Builder builder(Identifier identifier) {
        return new Builder(new IdentifierNameExpression(0, 0, identifier));
    }

    /**
     * Returns a builder for a statement whose target keeps the position it was written at. The
     * target is an assignment target like any other, and a diagnostic about the variable - an
     * undefined one - points at the name rather than at the statement.
     */
    public static Builder builder(IdentifierNameExpression identifierExpression) {
        return new Builder(identifierExpression);
    }

    public boolean inhibitNewline() {
        return inhibitNewline;
    }

    public String prompt() {
        return prompt;
    }

    public Identifier identifier() {
        return identifierExpression.getIdentifier();
    }

    /** Returns the target of this statement, with the position it was written at. */
    public IdentifierNameExpression identifierExpression() {
        return identifierExpression;
    }

    /**
     * Returns a new {@code LineInputStatement}, based on this, with the identifier updated.
     */
    public LineInputStatement withIdentifier(Identifier identifier) {
        return new LineInputStatement(line(), column(), inhibitNewline, prompt,
                identifierExpression.withIdentifier(identifier));
    }

    @Override
    public String toString() {
        return "LINE INPUT"
                + (inhibitNewline ? "; " : " ")
                + (prompt != null ? "\"" + prompt + "\"; " : "")
                + identifier().name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LineInputStatement that = (LineInputStatement) o;
        return inhibitNewline == that.inhibitNewline &&
                Objects.equals(prompt, that.prompt) &&
                Objects.equals(identifier(), that.identifier());
    }

    @Override
    public int hashCode() {
        return Objects.hash(inhibitNewline, prompt, identifier());
    }

    /**
     * A {@code LineInputStatement} builder class.
     */
    public static class Builder {

        private int line;
        private int column;
        private final IdentifierNameExpression identifierExpression;
        private boolean inhibitNewline;
        private String prompt;

        private Builder(IdentifierNameExpression identifierExpression) {
            this.identifierExpression = identifierExpression;
        }

        public Builder line(int line) {
            this.line = line;
            return this;
        }

        public Builder column(int column) {
            this.column = column;
            return this;
        }

        public Builder inhibitNewline(boolean inhibitNewline) {
            this.inhibitNewline = inhibitNewline;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public LineInputStatement build() {
            return new LineInputStatement(line, column, inhibitNewline, prompt, identifierExpression);
        }
    }
}
