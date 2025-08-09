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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import se.dykstrom.jcc.main.Language.COL

/**
 * Compile-and-run integration tests for COL.
 *
 * @author Johan Dykstrom
 */
@EnabledOnOs(OS.WINDOWS)
class ColCompileAndRunIT : AbstractIntegrationTests() {

    @Test
    fun shouldPrintlnLiteral() {
        val source = listOf(
            "call println(7)",
            "call println(-7)",
            "call println(5.3)",
            "call println(-5.3)",
            "call println(true)",
            "call println(false)",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath)
        runAndAssertSuccess(
            sourcePath,
            """
            7
            -7
            5.300000
            -5.300000
            -1
            0
            
            """.trimIndent(),
            0
        )
    }

    @Test
    fun shouldPrintlnExpressions() {
        val source = listOf(
            "call println(1 + 2 + 3)",
            "call println(7 - 3 - 10)",
            "call println(10_000 - 1_000)",
            "call println(0b00010)",
            "call println(0xfe)",
            "call println(.99)",
            "call println(1E9)",
            "call println(1 * 2 * 3)",
            "call println(10.0 / 2.0)",
            "call println(10 div 3)",
            "call println(10 mod 3)",
            "call println(10 * -(10 - 2))",
            "call println(6 & 3)",
            "call println(6 | 3)",
            "call println(6 ^ 3)",
            "call println(~0)",
            "call println(0 == 1)",
            "call println(0 != 1)",
            "call println(0 < 1)",
            "call println(0 <= 1)",
            "call println(0 > 1)",
            "call println(0 >= 1)",
            "call println(true and false)",
            "call println(false and true)",
            "call println(true or false)",
            "call println(false or true)",
            "call println(false xor false)",
            "call println(not false)"
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(
            sourceFile,
            "6\n-6\n9000\n2\n254\n0.990000\n1000000000.000000\n6\n5.000000\n3\n1\n-80\n2\n7\n5\n-1\n0\n-1\n-1\n-1\n0\n0\n0\n0\n-1\n-1\n0\n-1\n",
            0
        )
    }

    @Test
    fun shouldCallIntrinsicFunctions() {
        val source = listOf(
            "call println(ceil(3.7))",
            "call println(floor(3.7))",
            "call println(round(3.7))",
            "call println(round(-3.7))",
            "call println(trunc(3.7))",
            "call println(trunc(-3.7))",
            "call println(sqrt(4.0))",
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(
            sourceFile,
            """
            4.000000
            3.000000
            4.000000
            -4.000000
            3.000000
            -3.000000
            2.000000
            
            """.trimIndent(),
            0
        )
    }

    @Test
    fun shouldCallImportedFunction() {
        val source = listOf(
            "import msvcrt._abs64(i64) -> i64 as libc_abs",
            "call println(libc_abs(-3))"
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "3\n", 0)
    }

    @Test
    fun shouldCallImportedFunctionWithAliasType() {
        val source = listOf(
            "alias foo as i64",
            "import msvcrt._abs64(foo) -> foo as libc_abs",
            "call println(libc_abs(0 - 3))"
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "3\n", 0)
    }

    @Test
    fun shouldCallImportedFunctionFromJccBasic() {
        val source = listOf(
            "import jccbasic.sgn(f64) -> i64",
            "call println(sgn(-7.0))"
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "-1\n", 0)
    }

    @Test
    fun shouldCallImportedFunctionFromKernel32() {
        val source = listOf(
            "import kernel32.Sleep(i64) -> void as sleep",
            "call println(1)",
            "call sleep(100)",
            "call println(2)"
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "1\n2\n", 0)
    }

    @Test
    fun shouldCallUserDefinedFunction() {
        val source = listOf(
            "alias Integer as i64",
            "import msvcrt._abs64(Integer) -> Integer as libc_abs",
            "fun foo() -> Integer := bar()",
            "fun bar() -> Integer := libc_abs(17)",
            "fun tee() -> Integer := foo()",
            "call println(foo())",
            "call println(bar())",
            "call println(tee())"
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "17\n17\n17\n", 0)
    }

    @Test
    fun shouldCallUserDefinedFunctionWithArgs() {
        val source = listOf(
            "import jccbasic.cdbl(f64) -> f64",
            "",
            "call println(foo(-7.0))",
            "call println(bar(5.0, 3.0))",
            "call println(bar(-1.0, 5.0))",
            "call println(tee(-2.0))",
            "",
            "fun foo(a as f64) -> f64 := a * cdbl(a)",
            "fun bar(a as f64, b as f64) -> f64 := foo(a) + tee(b)",
            "fun tee(a as f64) -> f64 := -a"
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "49.000000\n22.000000\n-4.000000\n2.000000\n", 0)
    }

    @Test
    fun shouldCallFunctionsWithI32Args() {
        val source = listOf(
            "import msvcrt.abs(i32) -> i32 as libc_abs",
            "",
            "call println(libc_abs(i32(-5)))",
            "call println(bar(i32(-5)))",
            "",
            "fun bar(a as i32) -> i32 := a",
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "5\n-5\n", 0)
    }

    @Test
    fun shouldCallUserDefinedFunctionWithUserDefinedFunctionArg() {
        val source = listOf(
            "call println(foo(5))",
            "call println(bar(foo, 8))",
            "",
            "fun foo(a as i64) -> i64 := -a",
            "fun bar(f as (i64) -> i64, v as i64) -> i64 := f(v)",
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "-5\n-8\n", 0)
    }

    @Test
    fun shouldCallUserDefinedFunctionWithImportedFunctionArg() {
        val source = listOf(
            "import msvcrt._abs64(i64) -> i64 as libc_abs",
            "",
            "call println(libc_abs(-5))",
            "call println(bar(libc_abs, -8))",
            "",
            "fun bar(f as (i64) -> i64, v as i64) -> i64 := f(v)",
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "5\n8\n", 0)
    }

    @Test
    fun shouldCallUserDefinedFunctionWithUserDefinedFunctionArgReturnedFromOtherFunction() {
        val source = listOf(
            "call println(tee(foo, bar(foo), 3))",
            "",
            "fun foo(a as i64) -> i64 := a + 1",
            "fun bar(f as (i64) -> i64) -> (i64) -> i64 := f",
            "fun tee(x as (i64) -> i64, y as (i64) -> i64, z as i64) -> i64 := x(y(z))",
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "5\n", 0)
    }
}
