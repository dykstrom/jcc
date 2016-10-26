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

package se.dykstrom.jcc.common.error;

/**
 * Represents a single compilation error.
 *
 * @author Johan Dykstrom
 */
public class CompilationError implements Comparable<CompilationError> {

    private final int line;
    private final int column;
    private final String msg;
    private final Exception exception;

    public CompilationError(int line, int column, String msg, Exception exception) {
        this.line = line;
        this.column = column;
        this.msg = msg;
        this.exception = exception;
    }

    @Override
    public String toString() {
        return line + ":" + column + " " + msg;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(CompilationError that) {
        if (this.line < that.line) {
            return -1;
        } else if (this.line == that.line) {
            return 0;
        } else {
            return 1;
        }
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getMsg() {
        return msg;
    }

    public Exception getException() {
        return exception;
    }
}
