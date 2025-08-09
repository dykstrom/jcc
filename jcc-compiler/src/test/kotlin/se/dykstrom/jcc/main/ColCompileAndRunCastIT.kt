/*
 * Copyright (C) 2024 Johan Dykstrom
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
 * Compile-and-run integration tests for COL, specifically for testing casting.
 *
 * @author Johan Dykstrom
 */
@EnabledOnOs(OS.WINDOWS)
class ColCompileAndRunCastIT : AbstractIntegrationTests() {

    @Test
    fun shouldCastToInt() {
        val maxIntPlusOne = Integer.MAX_VALUE.toLong() + 1L
        val minIntMinusOne = Integer.MIN_VALUE.toLong() - 1L

        val source = listOf(
            // i64 -> i32
            "call println(i32(5))",
            "call println(i32(-5))",
            "call println(i32($maxIntPlusOne))",
            "call println(i32($minIntMinusOne))",

            // f64 -> i64
            "call println(i64(3.2))",
            "call println(i64(3.7))",
            "call println(i64(-3.7))",
        )
        // Compare casting result with Kotlin
        val maxIntPlusOneAsInt : Int = maxIntPlusOne.toInt()
        val minIntMinusOneAsInt : Int = minIntMinusOne.toInt()

        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "5\n-5\n$maxIntPlusOneAsInt\n$minIntMinusOneAsInt\n3\n3\n-3\n", 0)
    }

    @Test
    fun shouldCastToFloat() {
        val source = listOf(
            // i64 -> f64
            "call println(f64(3))",
            "call println(f64(-3))",

            // i64 --(explicit)-> i32 --(implicit)-> i64 --(explicit)-> f64
            "call println(f64(i32(-5)))",
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "3.000000\n-3.000000\n-5.000000\n", 0)
    }

    @Test
    fun shouldCallFunctionWithImplicitCast() {
        val source = listOf(
            "import msvcrt._abs64(i64) -> i64 as libc_abs",
            // Explicit cast to i32 followed by implicit cast to i64
            "call println(libc_abs(i32(-3)))",
        )
        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile)
        runAndAssertSuccess(sourceFile, "3\n", 0)
    }
}
