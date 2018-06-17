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

import static se.dykstrom.jcc.common.utils.FormatUtils.formatLineNumber;

/**
 * Represents an "ON GOSUB" statement such as '10 ON x GOSUB 100, 200, 300'.
 *
 * @author Johan Dykstrom
 */
public class OnGosubStatement extends AbstractOnJumpStatement {

    public OnGosubStatement(int line, int column, Expression expression, List<String> jumpLabels) {
        super(line, column, expression, jumpLabels);
    }

    public OnGosubStatement(int line, int column, Expression expression, List<String> jumpLabels, String label) {
        super(line, column, expression, jumpLabels, label);
    }

    @Override
    public String toString() {
        return formatLineNumber(getLabel()) + "ON " + getExpression() + " GOSUB " + toString(getJumpLabels());
    }
}
