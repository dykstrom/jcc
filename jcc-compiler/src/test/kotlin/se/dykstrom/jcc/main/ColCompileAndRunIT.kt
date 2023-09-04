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

package se.dykstrom.jcc.main

import org.junit.Test

/**
 * Compile-and-run integration tests for col.
 *
 * @author Johan Dykstrom
 */
class ColCompileAndRunIT : AbstractIntegrationTest() {

    @Test
    fun shouldPrintlnExpressions() {
        val source = listOf(
                "println 1 + 2 + 3",
                "println 10 - 7 - 3"
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "6\n0\n", 0)
    }
}
