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

package se.dykstrom.jcc.assembunny.ast;

import se.dykstrom.jcc.common.ast.AbstractNode;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.Statement;

import java.util.Objects;

/**
 * Represents a 'jump on not zero' statement such as 'jnz a -2'.
 *
 * @author Johan Dykstrom
 */
public class JnzStatement extends AbstractNode implements Statement {

    private final Expression expression;
    private final String target;

    public JnzStatement(int line, int column, Expression expression, String target) {
        super(line, column);
        this.expression = expression;
        this.target = target;
    }

    @Override
    public String toString() {
        return "jnz " + expression + " -> " + target;
    }

    public Expression getExpression() {
        return expression;
    }
    
    public String getTarget() {
        return target;
    }

    /**
     * Returns a copy of this JnzStatement, with the jump target updated.
     * 
     * @param target The new jump target.
     * @return The updated JnzStatement.
     */
    public JnzStatement withTarget(String target) {
        return new JnzStatement(line(), column(), expression, target);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JnzStatement that = (JnzStatement) o;
        return Objects.equals(this.expression, that.expression) && 
               Objects.equals(this.target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, target);
    }
}
