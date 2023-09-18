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
 * Compile-and-run integration tests for COL.
 *
 * @author Johan Dykstrom
 */
class ColCompileAndRunIT : AbstractIntegrationTest() {

    @Test
    fun shouldPrintlnExpressions() {
        val source = listOf(
                "println 1 + 2 + 3",
                "println 7 - 3 - 10",
                "println 10_000 - 1_000",
                "println .99",
                "println 1E9"
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "6\n-6\n9000\n0.990000\n1000000000.000000\n", 0)
    }

    /**
     * Note: COL does not yet support negation, so we use an expression
     * that results in a negative number as input to the abs function.
     */
    @Test
    fun shouldCallImportedFunction() {
        val source = listOf(
                "import msvcrt._abs64(i64) -> i64 as abs",
                "println abs(0 - 3)"
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "3\n", 0)
    }

    @Test
    fun shouldCallImportedFunctionWithAliasType() {
        val source = listOf(
                "alias foo = i64",
                "import msvcrt._abs64(foo) -> foo as abs",
                "println abs(0 - 3)"
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile, "-save-temps")
        runAndAssertSuccess(sourceFile, "3\n", 0)
    }

    @Test
    fun shouldCallImportedFunctionFromJccBasic() {
        val source = listOf(
                "import jccbasic.sgn(f64) -> i64",
                "println sgn(0.0 - 7.0)"
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "-1\n", 0)
    }

    /**
     * Note: We cannot just call a function as it is; it must be part of a statement.
     * And we cannot print something of type void. Therefore, we pretend the Sleep function
     * returns something even though it does not. In fact, it seems to always return 0,
     * so that is what we expect.
     */
    @Test
    fun shouldCallImportedFunctionFromKernel32() {
        val source = listOf(
                "import kernel32.Sleep(i64) -> i64 as sleep",
                "println 1",
                "println sleep(100)",
                "println 2"
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile, "-save-temps")
        runAndAssertSuccess(sourceFile, "1\n0\n2\n", 0)
    }
}
