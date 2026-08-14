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

import se.dykstrom.jcc.common.ast.AssignStatement;
import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.IdentifierExpression;

/**
 * Represents a copy statement such as 'cpy a b'.
 *
 * @author Johan Dykstrom
 */
public class CpyStatement extends AssignStatement {

    public CpyStatement(final int line, final int column, final Expression source, final IdentifierExpression destination) {
        super(line, column, destination, source);
    }

    /**
     * Returns a copy that is still a {@code CpyStatement}. The inherited implementations build a
     * plain {@link AssignStatement}, which silently downgrades this statement, and the optimizer
     * calls {@code withRhsExpression} on every assignment it walks. Once the node is no longer a
     * {@code CpyStatement}, {@code AssembunnyCodeGenerator} no longer recognizes it and lowers the
     * copy through the generic assign path rather than into the CPU register the Assembunny register
     * was allocated. That path also adds the register to the symbol table, so it reaches the data
     * section as a variable, even though an Assembunny register lives only in a CPU register.
     */
    @Override
    public CpyStatement withRhsExpression(final Expression rhsExpression) {
        return new CpyStatement(line(), column(), rhsExpression, getLhsExpression());
    }

    @Override
    public CpyStatement withLhsExpression(final IdentifierExpression lhsExpression) {
        return new CpyStatement(line(), column(), getRhsExpression(), lhsExpression);
    }

    @Override
    public String toString() {
        return "cpy " + getRhsExpression() + " " + getLhsExpression();
    }
}
