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
public record CompilationError(int line, int column, String msg, Exception exception) implements Comparable<CompilationError> {

    @Override
    public String toString() {
        return line + ":" + column + " " + msg;
    }

    @Override
    public int compareTo(CompilationError that) {
        return Integer.compare(this.line, that.line);
    }
}