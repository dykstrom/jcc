/*
 * Copyright (C) 2026 Johan Dykstrom
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

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.main.Language.BASIC

/**
 * Compile-and-run integration tests for BASIC, focusing on programs
 * compiled with optimization enabled.
 *
 * @author Johan Dykstrom
 */
class BasicCompileAndRunOptimizationIT : AbstractIntegrationTests() {

    @Test
    fun shouldMulWithPowerOfTwo() {
        val source = listOf(
            "a% = 7",
            "b% = a% * 4",
            "PRINT b%",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC, "-O1")
        runAndAssertSuccess(listOf(), listOf("28"))
    }

    @Test
    fun shouldOptimizeFirstUseOfVariable() {
        // The optimized statement is the first use of the variable,
        // so the code generator must define the variable as well
        val source = listOf(
            "a% = a% * 4",
            "PRINT a%",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC, "-O1")
        runAndAssertSuccess(listOf(), listOf("0"))
    }

    @Test
    fun shouldCompileDivisorThatFoldsToZero() {
        // Issue #120: at -O1 the divisor folds to zero, which must not fail the compilation
        val source = listOf(
            "a% = 0",
            "IF a% THEN",
            "  PRINT 1 \\ (1 - 1)",
            "  PRINT 1 MOD (1 - 1)",
            "END IF",
            "PRINT 1 / (1 - 1)",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(listOf(), listOf("inf"))
        compileAndAssertSuccess(sourcePath, BASIC, "-O1")
        runAndAssertSuccess(listOf(), listOf("inf"))
    }

    @Test
    fun shouldOptimizePrintWithTrailingSeparator() {
        val source = listOf(
            "PRINT 1 + 1;",
            "PRINT \"a\",",
            "PRINT",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC, "-O1")
        runAndAssertSuccess(listOf(), listOf("2a"))
    }

    @Test
    fun shouldUseConstantsInArraySizes() {
        val source = listOf(
            "CONST N = 2 * 3, LESS = N < 7",
            "DIM a%(N, N - 1)",
            "a%(N, N - 1) = LESS",
            "PRINT a%(N, N - 1)",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(listOf(), listOf("-1"))
        compileAndAssertSuccess(sourcePath, BASIC, "-O1")
        runAndAssertSuccess(listOf(), listOf("-1"))
    }

    @Test
    fun shouldFoldStringComparisonLikeStrcmp() {
        // A character above U+FFFF sorts after U+FFFD in UTF-8, which strcmp compares
        val source = listOf(
            "PRINT \"\uD83D\uDE00\" < \"\uFFFD\"",
            "PRINT \"\uD83D\uDE00\" > \"\uFFFD\"",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(listOf(), listOf("0", "-1"))
        compileAndAssertSuccess(sourcePath, BASIC, "-O1")
        runAndAssertSuccess(listOf(), listOf("0", "-1"))
    }

    @Test
    fun shouldNotDropFunctionCallInModOne() {
        // rnd(0.0) repeats the last random number, which is the one from the MOD expression
        val source = listOf(
            "DEFDBL f",
            "first = rnd(1.0)",
            "x% = cint(rnd(1.0) * 10) MOD 1",
            "PRINT x%; rnd(0.0) <> first",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(listOf(), listOf("0-1"))
        compileAndAssertSuccess(sourcePath, BASIC, "-O1")
        runAndAssertSuccess(listOf(), listOf("0-1"))
    }

    @Test
    fun shouldNotFoldFloatModOne() {
        val source = listOf(
            "DEFDBL f",
            "f = 2.5",
            "PRINT f MOD 1",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(listOf(), listOf("0.500000"))
        compileAndAssertSuccess(sourcePath, BASIC, "-O1")
        runAndAssertSuccess(listOf(), listOf("0.500000"))
    }

    @Test
    fun shouldOptimizeRoundedAssignment() {
        val source = listOf(
            "DIM i AS INTEGER",
            "i = 2.5 + 1",
            "PRINT i",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC, "-O1")
        runAndAssertSuccess(listOf(), listOf("4"))
    }

    @Test
    fun shouldNotReplaceParameterThatShadowsConstant() {
        val source = listOf(
            "CONST X = 5",
            "DEF FNA(X) = X * 2",
            "PRINT FNA(3)",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC, "-O1")
        runAndAssertSuccess(listOf(), listOf("6.000000"))
    }
}
