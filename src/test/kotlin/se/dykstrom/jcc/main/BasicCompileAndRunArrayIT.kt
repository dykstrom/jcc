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
            "print a%(0)"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0\n", 0)
    }

    @Test
    fun shouldDefineMultiDimensionalArray() {
        val source = listOf(
            "dim a%(10, 5, 2) as integer",
            "print a%(3, 2, 1)"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0\n", 0)
    }

    @Test
    fun shouldDefineThreeArrays() {
        val source = listOf(
            "dim a%(10) as integer",
            "dim b%(5) as integer",
            "dim c%(2) as integer",
            "print a%(0) ; b%(0) ; c%(0)"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "000\n", 0)
    }

    @Test
    fun shouldPrintAllElementsOfIntegerArray() {
        val source = listOf(
            "dim a%(3) as integer",
            "dim index as integer",
            "while index < 4",
            "  print a%(index)",
            "  index = index + 1",
            "wend"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0\n0\n0\n0\n", 0)
    }

    @Test
    fun shouldPrintAllElementsOfFloatArray() {
        val source = listOf(
            "dim a#(3) as double",
            "dim index as integer",
            "while index < 4",
            "  print a#(index)",
            "  index = index + 1",
            "wend"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0.000000\n0.000000\n0.000000\n0.000000\n", 0)
    }

    @Test
    fun shouldPrintAllElementsOfStringArray() {
        val source = listOf(
            "dim a$(3) as string",
            "dim index as integer",
            "while index < 4",
            "  print a$(index)",
            "  index = index + 1",
            "wend"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "\n\n\n\n", 0)
    }

    @Test
    fun subscriptsCanBeExpressions() {
        val source = listOf(
            "dim a%(10, 5) as integer",
            "dim b as integer",
            "let b = 1 + 4",
            "print a%(b - 1, abs(-2))"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0\n", 0)
    }
}
