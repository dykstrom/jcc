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

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.main.Language.BASIC

/**
 * Compile-and-run integration tests for BASIC, focusing on user-defined functions.
 *
 * @author Johan Dykstrom
 */
@Tag("LLVM")
class BasicCompileAndRunUserFunctionsIT : AbstractIntegrationTests() {

    @Test
    fun callExpressionFunctions() {
        val source = listOf(
            """
            DEF FNint1%(x AS INTEGER) = x
            DEF FNint2%(x AS INTEGER, y AS INTEGER) = x + y
            DEF FNfloat1(x AS DOUBLE) = x
            DEF FNfloat2(x AS DOUBLE, y AS DOUBLE) = x + y
            DEF FNmix#(x AS INTEGER, y AS DOUBLE) = x - y
            DEF FNstring1$(x AS STRING) = x
            DEF FNstring2$(x AS STRING, y AS STRING) = x + y

            PRINT FNint1%(7)
            PRINT FNint2%(8, 9)
            PRINT FNfloat1(7.0)
            PRINT FNfloat2(8.0, 9.0)
            PRINT FNmix#(5, 3.0)
            PRINT FNstring1$("foo")
            PRINT FNstring2$("foo", "bar")
            """
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(
            listOf(),
            listOf(
                "7",
                "17",
                "7.000000",
                "17.000000",
                "2.000000",
                "foo",
                "foobar",
            ),
        )
    }

    @Test
    fun functionShouldCallOtherFunction() {
        val source = listOf(
            """
            DEF FNone%() = abs(1)
            DEF FNtwo%(x AS INTEGER) = FNone%() + x
            DEF FNthree%(y AS INTEGER) = FNtwo%(FNone%() + y)

            PRINT FNthree%(7)
            PRINT FNthree%(-2)
            """
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(listOf(), listOf("9", "0"))
    }

    @Test
    fun callFunctionWithMoreThanFourArgs() {
        val source = listOf(
            """
            DEF fnfoo%(a as INTEGER, b as INTEGER, c as INTEGER, d as INTEGER, e as INTEGER) = a + b + c + d + e

            PRINT fnfoo%(10000, 2000, 300, 40, 5)
            PRINT fnfoo%(1, -1, 1, -1, 1)
            """
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(listOf(), listOf("12345", "1"))
    }

    @Test
    fun assignResultOfFunctionCall() {
        val source = listOf(
            """
            DEF FNfoo$(s AS STRING) = lcase$(s) + ucase$(s)

            DIM x AS STRING

            x = FNfoo$("Foo")
            PRINT x
            x = FNfoo$("Bar")
            PRINT x
            """
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(listOf(), listOf("fooFOO", "barBAR"))
    }
}
