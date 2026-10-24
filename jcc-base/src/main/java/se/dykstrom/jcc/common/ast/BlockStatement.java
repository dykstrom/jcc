/*
 * Copyright (C) 2026 Johan Dykstrom
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

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * A statement that holds one or more nested lists of statements, such as a loop body,
 * or the then and else branches of an IF statement.
 *
 * @author Johan Dykstrom
 */
public interface BlockStatement extends Statement {

    /**
     * Returns the nested statement lists, in a fixed order for each implementation.
     */
    List<List<Statement>> blocks();

    /**
     * Returns a copy of this statement with the nested statement lists replaced.
     *
     * @throws IllegalArgumentException If the number of lists differs from that of {@link #blocks()}.
     */
    BlockStatement withBlocks(List<List<Statement>> blocks);

    /**
     * Returns the expressions this statement holds itself, such as a condition.
     * The list never contains null.
     */
    List<Expression> expressions();

    /**
     * Returns a copy of this statement with each nested statement list replaced
     * by the result of applying {@code mapper} to it.
     */
    default BlockStatement mapBlocks(final UnaryOperator<List<Statement>> mapper) {
        return withBlocks(blocks().stream().map(mapper).toList());
    }
}
