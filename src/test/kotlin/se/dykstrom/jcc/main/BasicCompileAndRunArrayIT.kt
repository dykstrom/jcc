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
            "while index <= 3",
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
            "while index <= 3",
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
            "while index <= 3",
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

    @Test
    fun shouldSetAndGetIntegerArrayElement() {
        val source = listOf(
            "dim a%(10) as integer",
            "a%(3) = 9",
            "print a%(3)"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "9\n", 0)
    }

    @Test
    fun shouldSetAndGetFloatArrayElement() {
        val source = listOf(
            "dim f#(10) as double",
            "f#(4) = 9.7",
            "print f#(4)"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "9.700000\n", 0)
    }

    @Test
    fun shouldSetAndGetStringArrayElement() {
        val source = listOf(
            "dim s$(10) as string",
            "s$(9) = \"foo\"",
            "print s$(9)"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "foo\n", 0)
    }

    @Test
    fun shouldSetAndGetAllArrayElements() {
        val source = listOf(
            "dim a(7) as integer",
            "dim i as integer",
            "while i <= 7",
            "  a(i) = 10 - i",
            "  i = i + 1",
            "wend",
            "i = 0",
            "while i <= 7",
            "  print i; \"=\"; a(i)",
            "  i = i + 1",
            "wend"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0=10\n1=9\n2=8\n3=7\n4=6\n5=5\n6=4\n7=3\n", 0)
    }

    @Test
    fun shouldSetAndGetAllStringArrayElements() {
        val source = listOf(
            "dim s(7) as string",
            "dim i as integer",
            "",
            "i = 0",
            "while i <= 7",
            "  s(i) = \"i=\" + ltrim$(str$(i))",
            "  i = i + 1",
            "wend",
            "i = 0",
            "while i <= 7",
            "  print s(i)",
            "  i = i + 1",
            "wend",
            "",
            "i = 0",
            "while i <= 7",
            "  s(i) = \"i=\" + ltrim$(str$(i * 10))",
            "  i = i + 1",
            "wend",
            "i = 0",
            "while i <= 7",
            "  print s(i)",
            "  i = i + 1",
            "wend"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "i=0\ni=1\ni=2\ni=3\ni=4\ni=5\ni=6\ni=7\ni=0\ni=10\ni=20\ni=30\ni=40\ni=50\ni=60\ni=70\n", 0)
    }

    @Test
    fun shouldSetAndGetAll2DArrayElements() {
        val source = listOf(
            "dim a(2, 3) as integer",
            "dim x as integer, y as integer",
            "",
            "x = 0",
            "while x <= 2",
            "  y = 0",
            "  while y <= 3",
            "    a(x, y) = 10 * x + y",
            "    y = y + 1",
            "  wend",
            "  x = x + 1",
            "wend",
            "",
            "x = 0",
            "while x <= 2",
            "  y = 0",
            "  while y <= 3",
            "    print a(x, y)",
            "    y = y + 1",
            "  wend",
            "  x = x + 1",
            "wend",
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "0\n1\n2\n3\n10\n11\n12\n13\n20\n21\n22\n23\n", 0)
    }
}
