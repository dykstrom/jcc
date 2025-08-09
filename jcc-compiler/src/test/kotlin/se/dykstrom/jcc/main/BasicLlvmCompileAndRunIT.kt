/*
 * Copyright (C) 2025 Johan Dykstrom
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

package se.dykstrom.jcc.main

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.main.Language.BASIC

/**
 * Compile-and-run integration tests for the BASIC LLVM backend.
 *
 * @author Johan Dykstrom
 */
@Tag("LLVM")
class BasicLlvmCompileAndRunIT : AbstractIntegrationTests() {

    @Test
    fun shouldPrintLiterals() {
        val source = listOf(
            "REM Print literals!",
            "PRINT 7",
            "PRINT -7",
            "PRINT 5.3",
            "PRINT -5.3",
            "PRINT 1; 3.14; 1000",
            "PRINT \"foo\"",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "7",
                "-7",
                "5.300000",
                "-5.300000",
                "13.1400001000",
                "foo",
            ),
        )
    }
}
