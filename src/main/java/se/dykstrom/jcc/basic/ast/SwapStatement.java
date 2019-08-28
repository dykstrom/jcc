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
 * Represents a "SWAP" statement such as '10 SWAP a, b'.
 *
 * @author Johan Dykstrom
 */
public class SwapStatement extends Statement {

    private final Identifier first;
    private final Identifier second;

    public SwapStatement(int line, int column, Identifier first, Identifier second) {
        this(line, column, first, second, null);
    }

    public SwapStatement(int line, int column, Identifier first, Identifier second, String label) {
        super(line, column, label);
        this.first = first;
        this.second = second;
    }

    public Identifier getFirst() {
        return first;
    }

    public Identifier getSecond() {
        return second;
    }

    /**
     * Returns a new SwapStatement, based on this, with the first identifier updated.
     */
    public SwapStatement withFirst(Identifier first) {
        return new SwapStatement(getLine(), getColumn(), first, second, getLabel());
    }

    /**
     * Returns a new SwapStatement, based on this, with the second identifier updated.
     */
    public SwapStatement withSecond(Identifier second) {
        return new SwapStatement(getLine(), getColumn(), first, second, getLabel());
    }

    @Override
    public String toString() {
        return formatLineNumber(getLabel()) +  "SWAP " + first.getName() + ", " + second.getName();
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
