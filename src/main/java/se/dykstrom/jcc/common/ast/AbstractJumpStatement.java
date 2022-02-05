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

package se.dykstrom.jcc.common.ast;

import java.util.Objects;

/**
 * Abstract base class for different types of jump statements, such as GOTO or GOSUB.
 *
 * @author Johan Dykstrom
 */
public abstract class AbstractJumpStatement extends AbstractNode implements Statement {

    private final String jumpLabel;

    protected AbstractJumpStatement(int line, int column, String jumpLabel) {
        super(line, column);
        this.jumpLabel = jumpLabel;
    }

    /**
     * Returns the label to jump to.
     */
    public String getJumpLabel() {
        return jumpLabel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractJumpStatement that = (AbstractJumpStatement) o;
        return Objects.equals(jumpLabel, that.jumpLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jumpLabel);
    }
}
