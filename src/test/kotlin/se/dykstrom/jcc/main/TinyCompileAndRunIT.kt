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

package se.dykstrom.jcc.main

import org.junit.Test

/**
 * Compile-and-run integration tests for Tiny.
 *
 * @author Johan Dykstrom
 */
class TinyCompileAndRunIT : AbstractIntegrationTest() {

    @Test
    fun shouldWriteExpression() {
        val source = listOf("BEGIN WRITE 1 + 2 - 3 END")
        val sourceFile = createSourceFile(source, TINY)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0\n")
    }

    @Test
    fun shouldOptimizeAddOne() {
        val source = listOf(
                "BEGIN",
                "  a := 0",
                "  a:= a + 1",
                "  WRITE a",
                "END"
        )
        val sourceFile = createSourceFile(source, TINY)
        compileAndAssertSuccess(sourceFile, "-O1")
        runAndAssertSuccess(sourceFile, "1\n")
    }
}
