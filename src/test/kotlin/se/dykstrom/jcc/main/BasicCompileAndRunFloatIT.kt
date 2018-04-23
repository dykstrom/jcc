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
                "PRINT 3.14",
                "PRINT 1.0",
                "PRINT 1E3",
                "PRINT 0.17",
                "PRINT 1.23D-2"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "3.140000\n1.000000\n1000.000000\n0.170000\n0.012300\n", 0)
    }

    @Test
    fun shouldPrintFloatOnlyExpressions() {
        val source = listOf(
                "PRINT 1.2 + 3.4",
                "PRINT 5.0 - 3.5",
                "PRINT 1E3 / 10.0",
                "PRINT 0.17 * 100.0",
                "PRINT 1.23D-2 * 2.0 + 1.0",
                "PRINT 18.5 MOD 4.2"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "4.600000\n1.500000\n100.000000\n17.000000\n1.024600\n1.700000\n", 0)
    }

    @Test
    fun shouldPrintMixedExpressions() {
        val source = listOf(
                "PRINT 1.2 + 3 - 7.3",
                "PRINT 5 - 3.5 * 2",
                "PRINT 1E3 / (10.0 - 5)",
                "PRINT 0.17 * 100 + 23",
                "PRINT 1.23D-2 * 2 + 1",
                "PRINT 18.5 MOD 4; -18.5 MOD 4; 18.5 MOD -4; -18.5 MOD -4"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "-3.100000\n-2.000000\n200.000000\n40.000000\n1.024600\n2.500000-2.5000002.500000-2.500000\n", 0)
    }

    @Test
    fun shouldPrintMultipleArgs() {
        val source = listOf(
                "PRINT \"a = \"; 3.14",
                "PRINT \"(1. + 2) * 3.\"; \" = \"; (1. + 2) * 3.",
                "PRINT 1.0, 2.0, 3.0, 4.0, 5.0"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "a = 3.140000\n(1. + 2) * 3. = 9.000000\n1.0000002.0000003.0000004.0000005.000000\n")
    }

    @Test
    fun shouldAssignFloats() {
        val source = listOf(
                "10 let f1 = 10 / 5",
                "20 let f2 = 0.5 * 5",
                "30 print f1 ; \" \" ; f2",
                "40 let f3 = 10 * f1 * f2",
                "50 print f3"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "2.000000 2.500000\n50.000000\n")
    }

    @Test
    fun shouldPrintAndReassign() {
        val source = listOf(
                "10 let a = 7.0",
                "20 print \"a=\"; a",
                "30 let a = a + 1",
                "40 print \"a=\"; a"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "a=7.000000\na=8.000000\n")
    }

    @Test
    fun shouldPrintBooleanExpressions() {
        val source = listOf(
                "10 let a = 7.0 : print a",
                "20 let b = 5.1 : print b",
                "25 let c = 3 : print c",
                "30 let eq = a = b : print eq",
                "40 let ne = a <> b : print ne",
                "50 let gt = a > b : print gt",
                "60 let ge = a >= b : print ge",
                "70 let lt = a < b : print lt",
                "80 let le = a <= b : print le",
                "90 print a = a",
                "100 print a = c",
                "105 print c = a",
                "110 print a > c",
                "120 print c > a",
                "130 print c < a",
                "140 print a < c"
        )
        val sourceFile = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "7.000000\n5.100000\n3\n0\n-1\n-1\n-1\n0\n0\n-1\n0\n0\n-1\n0\n-1\n0\n")
    }
}
