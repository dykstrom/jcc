/*
 * Copyright (C) 2024 Johan Dykstrom
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

import se.dykstrom.jcc.common.ast.AstProgram;
import se.dykstrom.jcc.common.code.TargetProgram;

/**
 * Interface to be implemented by all code generators.
 */
public interface CodeGenerator {
    /**
     * Generates code for the given {@code program} in AST format.
     *
     * @param program The program to generate code for.
     * @return The generated code, in the form of a target language program, e.g. assembly code.
     */
    TargetProgram generate(final AstProgram program);

    TypeManager typeManager();
}
