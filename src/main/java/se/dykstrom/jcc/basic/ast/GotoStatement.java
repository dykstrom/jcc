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

import se.dykstrom.jcc.common.ast.Statement;

import java.util.Objects;

/**
 * Represents a goto statement such as '10 GOTO 20'.
 *
 * @author Johan Dykstrom
 */
public class GotoStatement extends Statement {

    private final String gotoLine;

    public GotoStatement(int line, int column, String gotoLine) {
        this(line, column, gotoLine, null);
    }

    public GotoStatement(int line, int column, String gotoLine, String label) {
        super(line, column, label);
        this.gotoLine = gotoLine;
    }

    @Override
    public String toString() {
        return formatLineNumber(getLabel()) + " GOTO " + gotoLine;
    }

    private String formatLineNumber(String lineNumber) {
        return (lineNumber != null) ? lineNumber : "<line>";
    }

    /**
     * Returns the line to go to.
     */
    public String getGotoLine() {
        return gotoLine;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GotoStatement that = (GotoStatement) o;
        return Objects.equals(gotoLine, that.gotoLine) && Objects.equals(getLabel(), that.getLabel());
    }

    @Override
    public int hashCode() {
        return Objects.hash(gotoLine, getLabel());
    }
}
