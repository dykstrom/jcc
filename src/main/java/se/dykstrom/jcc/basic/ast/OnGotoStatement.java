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

import static java.util.stream.Collectors.joining;
import static se.dykstrom.jcc.common.utils.FormatUtils.formatLineNumber;

import java.util.List;
import java.util.Objects;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.Statement;

/**
 * Represents an "ON GOTO" statement such as '10 ON x GOTO 100, 200, 300'.
 *
 * @author Johan Dykstrom
 */
public class OnGotoStatement extends Statement {

    private final Expression expression;
    private final List<String> gotoLabels;

    public OnGotoStatement(int line, int column, Expression expression, List<String> gotoLabels) {
        this(line, column, expression, gotoLabels, null);
    }


    public OnGotoStatement(int line, int column, Expression expression, List<String> gotoLabels, String label) {
        super(line, column, label);
        this.expression = expression;
        this.gotoLabels = gotoLabels;
    }

    @Override
    public String toString() {
        return formatLineNumber(getLabel()) + "ON " + expression + " GOTO " + toString(gotoLabels);
    }

    private String toString(List<String> gotoLabels) {
        return gotoLabels.stream().collect(joining(", "));
    }


    public Expression getExpression() {
        return expression;
    }
    
    /**
     * Returns the list of goto labels.
     */
    public List<String> getGotoLabels() {
        return gotoLabels;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OnGotoStatement that = (OnGotoStatement) o;
        return Objects.equals(this.expression, that.expression) && Objects.equals(this.gotoLabels, that.gotoLabels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, gotoLabels);
    }
}
