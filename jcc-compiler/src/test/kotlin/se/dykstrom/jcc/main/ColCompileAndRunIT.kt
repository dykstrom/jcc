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
import se.dykstrom.jcc.main.Language.COL

/**
 * Compile-and-run integration tests for COL.
 *
 * @author Johan Dykstrom
 */
class ColCompileAndRunIT : AbstractIntegrationTests() {

    @Test
    fun shouldPrintlnExpressions() {
        val source = listOf(
                "println 1 + 2 + 3",
                "println 7 - 3 - 10",
                "println 10_000 - 1_000",
                "println .99",
                "println 1E9",
                "println 1 * 2 * 3",
                "println 10 / 2.0",
                "println 10 div 3",
                "println 10 mod 3",
                "println 10 * -(10 - 2)"
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(
            sourceFile,
            "6\n-6\n9000\n0.990000\n1000000000.000000\n6\n5.000000\n3\n1\n-80\n",
            0
        )
    }

    @Test
    fun shouldCallImportedFunction() {
        val source = listOf(
                "import msvcrt._abs64(i64) -> i64 as abs",
                "println abs(-3)"
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
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "3\n", 0)
    }

    @Test
    fun shouldCallImportedFunctionFromJccBasic() {
        val source = listOf(
                "import jccbasic.sgn(f64) -> i64",
                "println sgn(-7.0)"
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "-1\n", 0)
    }

    @Test
    fun shouldCallImportedFunctionFromKernel32() {
        val source = listOf(
                "import kernel32.Sleep(i64) -> void as sleep",
                "println 1",
                "sleep(100)",
                "println 2"
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile, "-save-temps")
        runAndAssertSuccess(sourceFile, "1\n2\n", 0)
    }
}
