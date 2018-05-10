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

package se.dykstrom.jcc.common.ast;

import java.util.Objects;

import static se.dykstrom.jcc.common.utils.FormatUtils.formatLineNumber;

/**
 * Represents a comment statement such as '10 REM ...'.
 *
 * @author Johan Dykstrom
 */
public class CommentStatement extends Statement {

    private final String text;

    public CommentStatement(int line, int column) {
        this(line, column, null);
    }

    public CommentStatement(int line, int column, String label) {
        this(line, column, null, label);
    }

    public CommentStatement(int line, int column, String text, String label) {
        super(line, column, label);
        this.text = text;
    }

    @Override
    public String toString() {
        return formatLineNumber(getLabel()) + "REM " + (text != null ? text : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommentStatement that = (CommentStatement) o;
        return Objects.equals(getLabel(), that.getLabel()) && Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLabel(), text);
    }
}
