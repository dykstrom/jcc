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

import se.dykstrom.jcc.common.ast.Program;
import se.dykstrom.jcc.common.error.SyntaxException;

import java.io.InputStream;

/**
 * Interface to be implemented by all syntax parsers.
 */
public interface SyntaxParser {

    /**
     * Parses the source code read from the given input stream, and creates an
     * AST program that represents the code.
     */
    Program parse(final InputStream inputStream) throws SyntaxException;
}
