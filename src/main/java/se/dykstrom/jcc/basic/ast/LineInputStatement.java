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

package se.dykstrom.jcc.basic.ast;

import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.types.Identifier;

import java.util.Objects;

import static se.dykstrom.jcc.common.utils.FormatUtils.formatLineNumber;

/**
 * Represents an "LINE INPUT" statement such as '10 LINE INPUT "Enter first name:"; name$'.
 *
 * @author Johan Dykstrom
 */
public class LineInputStatement extends Statement {

    private final boolean inhibitNewline;
    private final String prompt;
    private final Identifier identifier;

    private LineInputStatement(int line, int column, String label, boolean inhibitNewline, String prompt, Identifier identifier) {
        super(line, column, label);
        this.inhibitNewline = inhibitNewline;
        this.prompt = prompt;
        this.identifier = identifier;
    }

    /**
     * Returns a builder that can be used to build {@code LineInputStatement} objects.
     */
    public static Builder builder(Identifier identifier) {
        return new Builder(identifier);
    }

    public boolean inhibitNewline() {
        return inhibitNewline;
    }

    public String prompt() {
        return prompt;
    }

    public Identifier identifier() {
        return identifier;
    }

    /**
     * Returns a new {@code LineInputStatement}, based on this, with the identifier updated.
     */
    public LineInputStatement withIdentifier(Identifier identifier) {
        return new LineInputStatement(getLine(), getColumn(), getLabel(), inhibitNewline, prompt, identifier);
    }

    @Override
    public String toString() {
        return formatLineNumber(getLabel()) +  "LINE INPUT"
                + (inhibitNewline ? "; " : " ")
                + (prompt != null ? "\"" + prompt + "\"; " : "")
                + identifier.getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LineInputStatement that = (LineInputStatement) o;
        return inhibitNewline == that.inhibitNewline &&
                Objects.equals(prompt, that.prompt) &&
                Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inhibitNewline, prompt, identifier);
    }

    /**
     * A {@code LineInputStatement} builder class.
     */
    public static class Builder {

        private int line;
        private int column;
        private String label;
        private final Identifier identifier;
        private boolean inhibitNewline;
        private String prompt;

        private Builder(Identifier identifier) {
            this.identifier = identifier;
        }

        public Builder line(int line) {
            this.line = line;
            return this;
        }

        public Builder column(int column) {
            this.column = column;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
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
            return new LineInputStatement(line, column, label, inhibitNewline, prompt, identifier);
        }
    }
}
