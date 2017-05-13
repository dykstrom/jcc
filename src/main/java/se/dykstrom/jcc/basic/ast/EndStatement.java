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

import se.dykstrom.jcc.common.ast.ExitStatement;

import static se.dykstrom.jcc.common.utils.FormatUtils.formatLineNumber;

/**
 * Represents an end statement such as '10 END'.
 *
 * @author Johan Dykstrom
 */
public class EndStatement extends ExitStatement {

    public EndStatement(int line, int column) {
        this(line, column, null);
    }

    public EndStatement(int line, int column, String label) {
        super(line, column, 0, label);
    }

    @Override
    public String toString() {
        return formatLineNumber(getLabel()) + " END";
    }
}
