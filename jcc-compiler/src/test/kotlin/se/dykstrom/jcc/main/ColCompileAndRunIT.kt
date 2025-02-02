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
    fun shouldPrintlnExpressions() {
        val source = listOf(
            "println 1 + 2 + 3",
            "println 7 - 3 - 10",
            "println 10_000 - 1_000",
            "println .99",
            "println 1E9",
            "println 1 * 2 * 3",
            "println 10.0 / 2.0",
            "println 10 div 3",
            "println 10 mod 3",
            "println 10 * -(10 - 2)",
            "println 6 & 3",
            "println 6 | 3",
            "println 6 ^ 3",
            "println ~0",
            "println 0 == 1",
            "println 0 != 1",
            "println 0 < 1",
            "println 0 <= 1",
            "println 0 > 1",
            "println 0 >= 1",
            "println true and false",
            "println false and true",
            "println true or false",
            "println false or true",
            "println false xor false",
            "println not false"
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(
            sourceFile,
            "6\n-6\n9000\n0.990000\n1000000000.000000\n6\n5.000000\n3\n1\n-80\n2\n7\n5\n-1\n0\n-1\n-1\n-1\n0\n0\n0\n0\n-1\n-1\n0\n-1\n",
            0
        )
    }

    @Test
    fun shouldCallIntrinsicFunctions() {
        val source = listOf(
                "println ceil(3.7)",
                "println floor(3.7)",
                "println round(3.7)",
                "println round(-3.7)",
                "println trunc(3.7)",
                "println trunc(-3.7)",
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "4.000000\n3.000000\n4.000000\n-4.000000\n3.000000\n-3.000000\n", 0)
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
                "alias foo as i64",
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
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "1\n2\n", 0)
    }

    @Test
    fun shouldCallUserDefinedFunction() {
        val source = listOf(
            "alias Integer as i64",
            "import msvcrt._abs64(Integer) -> Integer as abs",
            "fun foo() -> Integer = bar()",
            "fun bar() -> Integer = abs(17)",
            "fun tee() -> Integer = foo()",
            "println foo()",
            "println bar()",
            "println tee()"
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
            "println foo(-7.0)",
            "println bar(5.0, 3.0)",
            "println bar(-1.0, 5.0)",
            "println tee(-2.0)",
            "",
            "fun foo(a as f64) -> f64 = a * cdbl(a)",
            "fun bar(a as f64, b as f64) -> f64 = foo(a) + tee(b)",
            "fun tee(a as f64) -> f64 = -a"
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "49.000000\n22.000000\n-4.000000\n2.000000\n", 0)
    }

    @Test
    fun shouldCallFunctionsWithI32Args() {
        val source = listOf(
            "import msvcrt.abs(i32) -> i32 as abs",
            "",
            "println abs(i32(-5))",
            "println bar(i32(-5))",
            "",
            "fun bar(a as i32) -> i32 = a",
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile, "-save-temps")
        runAndAssertSuccess(sourceFile, "5\n-5\n", 0)
    }

    @Test
    fun shouldCallUserDefinedFunctionWithUserDefinedFunctionArg() {
        val source = listOf(
            "println foo(5)",
            "println bar(foo, 8)",
            "",
            "fun foo(a as i64) -> i64 = -a",
            "fun bar(f as (i64) -> i64, v as i64) -> i64 = f(v)",
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "-5\n-8\n", 0)
    }

    @Test
    fun shouldCallUserDefinedFunctionWithImportedFunctionArg() {
        val source = listOf(
            "import msvcrt._abs64(i64) -> i64 as abs",
            "",
            "println abs(-5)",
            "println bar(abs, -8)",
            "",
            "fun bar(f as (i64) -> i64, v as i64) -> i64 = f(v)",
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "5\n8\n", 0)
    }

    @Test
    fun shouldCallUserDefinedFunctionWithUserDefinedFunctionArgReturnedFromOtherFunction() {
        val source = listOf(
            "println tee(foo, bar(foo), 3)",
            "",
            "fun foo(a as i64) -> i64 = a + 1",
            "fun bar(f as (i64) -> i64) -> (i64) -> i64 = f",
            "fun tee(x as (i64) -> i64, y as (i64) -> i64, z as i64) -> i64 = x(y(z))",
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "5\n", 0)
    }
}
