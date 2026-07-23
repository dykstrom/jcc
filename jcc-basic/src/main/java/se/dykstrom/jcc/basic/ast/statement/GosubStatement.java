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

package se.dykstrom.jcc.basic.ast.statement;

import se.dykstrom.jcc.common.ast.AbstractJumpStatement;

/**
 * Represents a GOSUB statement such as '10 GOSUB 20'.
 *
 * @author Johan Dykstrom
 */
public class GosubStatement extends AbstractJumpStatement {

    private final String nextLabel;

    public GosubStatement(int line, int column, String jumpLabel, String nextLabel) {
        super(line, column, jumpLabel);
        this.nextLabel = nextLabel;
    }

    public GosubStatement(final int line, final int column, final String jumpLabel) {
        this(line, column, jumpLabel, null);
    }

    public GosubStatement(final String jumpLabel) {
        this(0, 0, jumpLabel, null);
    }

    @Override
    public String toString() {
        return "GOSUB " + getJumpLabel();
    }

    public GosubStatement withNextLabel(final String nextLabel) {
        return new GosubStatement(line(), column(), getJumpLabel(), nextLabel);
    }

    /**
     * Returns the label of the statement that follows this GOSUB statement.
     * This field is set and used during LLVM IR code generation.
     */
    public String nextLabel() {
        return nextLabel;
    }
}
