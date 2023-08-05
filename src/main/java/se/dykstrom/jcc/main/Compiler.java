/*
 * Copyright (C) 2016 Johan Dykstrom
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

package se.dykstrom.jcc.main;

import se.dykstrom.jcc.common.intermediate.IntermediateProgram;

import java.nio.file.Path;

/**
 * Defines operations that should be implemented by all compilers.
 *
 * @author Johan Dykstrom
 */
public interface Compiler {

    /**
     * Compiles the source code read from the ANTLR input stream into an assembly code program.
     */
    IntermediateProgram compile();

    /**
     * Returns the path of the source file that this compiler operates on.
     */
    Path getSourcePath();
}
