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

package se.dykstrom.jcc.main;

import se.dykstrom.jcc.common.code.TargetProgram;

import java.nio.file.Path;

/**
 * Specifies operations to be implemented by all assemblers, or backend compilers.
 */
public interface Assembler {

    /**
     * Assembles the given target language program and writes the
     * result to the outputPath. The term assemble is used very broadly
     * here. The "assembling" can be performed by a compiler if the
     * target language is for example Java.
     */
    void assemble(final TargetProgram program,
                  final Path sourcePath,
                  final Path outputPath);
}
