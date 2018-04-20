/*
 * Copyright (C) 2018 Johan Dykstrom
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
 * Compile-and-run integration tests for Basic, specifically for testing floating point operations.
 *
 * @author Johan Dykstrom
 */
class BasicCompileAndRunFloatIT : AbstractIntegrationTest() {

    @Test
    fun shouldPrintLiteralExpressions() {
        val source = listOf(
                "10 PRINT 3.14",
                "20 PRINT 1.0",
                "30 PRINT 1E3"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "3.140000\n1.000000\n1000.000000\n", 0)
    }
}
