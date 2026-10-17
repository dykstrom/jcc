/*
 * Copyright (C) 2026 Johan Dykstrom
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

import static java.util.Objects.requireNonNull;

/**
 * Represents a statement that leaves the innermost enclosing loop of a given kind,
 * such as 'EXIT FOR'. The kind is the class of the loop statement to leave, and
 * matches any loop statement whose class is assignable to it.
 *
 * @author Johan Dykstrom
 */
public class LoopExitStatement extends AbstractNode implements Statement {

    private final Class<? extends Statement> loop;

    public LoopExitStatement(final int line, final int column, final Class<? extends Statement> loop) {
        super(line, column);
        this.loop = requireNonNull(loop);
    }

    public LoopExitStatement(final Class<? extends Statement> loop) {
        this(0, 0, loop);
    }

    /**
     * Returns the kind of loop to leave.
     */
    public Class<? extends Statement> getLoop() {
        return loop;
    }

    @Override
    public String toString() {
        return "EXIT " + loop.getSimpleName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoopExitStatement that = (LoopExitStatement) o;
        return Objects.equals(loop, that.loop);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loop);
    }
}
