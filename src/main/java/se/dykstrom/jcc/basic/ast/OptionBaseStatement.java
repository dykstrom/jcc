/*
 * Copyright (C) 2022 Johan Dykstrom
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
import se.dykstrom.jcc.common.ast.Statement;

import java.util.Objects;

/**
 * Represents an OPTION BASE statement such as 'OPTION BASE 1'.
 *
 * @author Johan Dykstrom
 */
public class OptionBaseStatement extends AbstractNode implements Statement {

    private final int base;

    public OptionBaseStatement(final int line, final int column, final int base) {
        super(line, column);
        this.base = base;
    }

    public int base() {
        return base;
    }

    @Override
    public String toString() {
        return "OPTION BASE " + base;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OptionBaseStatement that = (OptionBaseStatement) o;
        return base == that.base;
    }

    @Override
    public int hashCode() {
        return Objects.hash(base);
    }
}
