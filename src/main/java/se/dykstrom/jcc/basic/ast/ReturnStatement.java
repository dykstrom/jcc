/*
 * Copyright (C) 2018 Johan Dykstrom
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

/**
 * Represents a RETURN statement such as '100 RETURN'.
 *
 * @author Johan Dykstrom
 */
public class ReturnStatement extends AbstractNode implements Statement {

    public ReturnStatement(int line, int column) {
        super(line, column);
    }

    @Override
    public String toString() {
        return "RETURN";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ReturnStatement;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
