/*
 * Copyright (C) 2020 Johan Dykstrom
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
 * Compile-and-run integration tests for Basic, specifically for testing arrays.
 *
 * @author Johan Dykstrom
 */
class BasicCompileAndRunArrayIT : AbstractIntegrationTest() {

    @Test
    fun shouldDefineIntegerArray() {
        val source = listOf(
                "dim a%(10) as integer",
                "print 7"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "7\n", 0)
    }

    @Test
    fun shouldDefineMultiDimensionalArray() {
        val source = listOf(
                "dim a%(10, 5, 2) as integer",
                "print 7"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "7\n", 0)
    }

    @Test
    fun shouldDefineThreeArrays() {
        val source = listOf(
                "dim a%(10) as integer",
                "dim b%(5) as integer",
                "dim c%(2) as integer",
                "print 7"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "7\n", 0)
    }
}
