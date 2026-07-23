/*
 * Copyright (C) 2017 Johan Dykstrom
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

import se.dykstrom.jcc.common.ast.Expression;

import java.util.List;

/**
 * Represents an "ON GOSUB" statement such as '10 ON x GOSUB 100, 200, 300'.
 *
 * @author Johan Dykstrom
 */
public class OnGosubStatement extends AbstractOnJumpStatement {

    private final String nextLabel;

    public OnGosubStatement(final int line,
                            final int column,
                            final Expression expression,
                            final List<String> jumpLabels,
                            final String nextLabel) {
        super(line, column, expression, jumpLabels);
        this.nextLabel = nextLabel;
    }

    public OnGosubStatement(final int line, final int column, final Expression expression, final List<String> jumpLabels) {
        this(line, column, expression, jumpLabels, null);
    }

    public OnGosubStatement(final Expression expression, final List<String> jumpLabels) {
        this(0, 0, expression, jumpLabels, null);
    }

    @Override
    public String toString() {
        return "ON " + getExpression() + " GOSUB " + toString(getJumpLabels());
    }

    public OnGosubStatement withNextLabel(final String nextLabel) {
        return new OnGosubStatement(line(), column(), getExpression(), getJumpLabels(), nextLabel);
    }

    /**
     * Returns the label of the statement that follows this ON GOSUB statement.
     * This field is set and used during LLVM IR code generation.
     */
    public String nextLabel() {
        return nextLabel;
    }
}
