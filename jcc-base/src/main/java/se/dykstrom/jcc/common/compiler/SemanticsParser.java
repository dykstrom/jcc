/*
 * Copyright (C) 2023 Johan Dykstrom
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

package se.dykstrom.jcc.common.compiler;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.ast.Statement;
import se.dykstrom.jcc.common.error.SemanticsException;

/**
 * Interface to be implemented by all semantic parsers.
 */
public interface SemanticsParser {
    /**
     * Parses the given AST program, and checks that it is semantically correct.
     * Returns a possibly updated program with improved type information etc.
     */
    Program parse(final Program program) throws SemanticsException;

    default Statement statement(final Statement statement) { return statement; }

    default Expression expression(final Expression expression) { return expression; }

    void reportSemanticsError(int line, int column, String msg, SemanticsException exception);
}
