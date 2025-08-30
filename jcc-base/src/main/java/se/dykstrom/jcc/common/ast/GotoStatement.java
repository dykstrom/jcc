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

/**
 * Represents a GOTO statement such as '10 GOTO 20'.
 *
 * @author Johan Dykstrom
 */
public class GotoStatement extends AbstractJumpStatement {

    public GotoStatement(final int line, final int column, final String jumpLabel) {
        super(line, column, jumpLabel);
    }

    public GotoStatement(final String jumpLabel) {
        this(0, 0, jumpLabel);
    }

    @Override
    public String toString() {
        return "GOTO " + getJumpLabel();
    }
}
