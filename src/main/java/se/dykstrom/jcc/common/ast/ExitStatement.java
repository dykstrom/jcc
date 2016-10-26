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

/**
 * Represents an exit statement.
 *
 * @author Johan Dykstrom
 */
public class ExitStatement extends Statement {

    private final int status;

    public ExitStatement(int line, int column, int status) {
        this(line, column, status, null);
    }

    public ExitStatement(int line, int column, int status, String label) {
        super(line, column, label);
        this.status = status;
    }

    /**
     * Returns the exit status.
     */
    public int getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "exit(" + status + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExitStatement that = (ExitStatement) o;
        return Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status);
    }
}
