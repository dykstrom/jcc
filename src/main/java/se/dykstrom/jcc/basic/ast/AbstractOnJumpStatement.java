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

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.Statement;

import java.util.List;
import java.util.Objects;

/**
 * Abstract base class for different types of on-jump statements, such as ON-GOTO.
 *
 * @author Johan Dykstrom
 */
public abstract class AbstractOnJumpStatement extends Statement {

    private final Expression expression;
    private final List<String> jumpLabels;

    AbstractOnJumpStatement(int line, int column, Expression expression, List<String> jumpLabels) {
        this(line, column, expression, jumpLabels, null);
    }

    AbstractOnJumpStatement(int line, int column, Expression expression, List<String> jumpLabels, String label) {
        super(line, column, label);
        this.expression = expression;
        this.jumpLabels = jumpLabels;
    }

    /**
     * Returns a string representing the given list of jump labels.
     */
    String toString(List<String> jumpLabels) {
        return String.join(", ", jumpLabels);
    }

    public Expression getExpression() {
        return expression;
    }
    
    /**
     * Returns the list of jump labels.
     */
    public List<String> getJumpLabels() {
        return jumpLabels;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractOnJumpStatement that = (AbstractOnJumpStatement) o;
        return Objects.equals(this.expression, that.expression) && Objects.equals(this.jumpLabels, that.jumpLabels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, jumpLabels);
    }
}
