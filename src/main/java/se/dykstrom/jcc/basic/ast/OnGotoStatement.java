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

package se.dykstrom.jcc.basic.ast;

import se.dykstrom.jcc.common.ast.Expression;

import java.util.List;

/**
 * Represents an "ON GOTO" statement such as '10 ON x GOTO 100, 200, 300'.
 *
 * @author Johan Dykstrom
 */
public class OnGotoStatement extends AbstractOnJumpStatement {

    public OnGotoStatement(int line, int column, Expression expression, List<String> jumpLabels) {
        super(line, column, expression, jumpLabels);
    }

    @Override
    public String toString() {
        return "ON " + getExpression() + " GOTO " + toString(getJumpLabels());
    }
}
