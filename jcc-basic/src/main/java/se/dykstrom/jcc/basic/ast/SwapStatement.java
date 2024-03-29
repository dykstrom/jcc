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

import se.dykstrom.jcc.common.ast.AbstractNode;
import se.dykstrom.jcc.common.ast.IdentifierExpression;
import se.dykstrom.jcc.common.ast.Statement;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Represents a "SWAP" statement such as '10 SWAP a, b'.
 *
 * @author Johan Dykstrom
 */
public class SwapStatement extends AbstractNode implements Statement {

    private final IdentifierExpression first;
    private final IdentifierExpression second;

    public SwapStatement(int line, int column, IdentifierExpression first, IdentifierExpression second) {
        super(line, column);
        this.first = requireNonNull(first);
        this.second = requireNonNull(second);
    }

    public IdentifierExpression first() {
        return first;
    }

    public IdentifierExpression second() {
        return second;
    }

    /**
     * Returns a new SwapStatement, based on this, with the first identifier updated.
     */
    public SwapStatement withFirst(IdentifierExpression first) {
        return new SwapStatement(line(), column(), first, second);
    }

    /**
     * Returns a new SwapStatement, based on this, with the second identifier updated.
     */
    public SwapStatement withSecond(IdentifierExpression second) {
        return new SwapStatement(line(), column(), first, second);
    }

    @Override
    public String toString() {
        return "SWAP " + first + ", " + second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SwapStatement that = (SwapStatement) o;
        return Objects.equals(first, that.first) && Objects.equals(second, that.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }
}
