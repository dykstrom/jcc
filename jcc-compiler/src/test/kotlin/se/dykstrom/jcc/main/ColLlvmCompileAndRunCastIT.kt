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

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.main.Language.COL

/**
 * Compile-and-run integration tests for the COL LLVM backend, specifically for testing casting.
 *
 * @author Johan Dykstrom
 */
@Tag("LLVM")
class ColLlvmCompileAndRunCastIT : AbstractIntegrationTests() {

    @Test
    fun shouldCastToInt() {
        val maxIntPlusOne = Integer.MAX_VALUE.toLong() + 1L
        val minIntMinusOne = Integer.MIN_VALUE.toLong() - 1L

        val source = listOf(
            // f64 -> i32
            "call println(i32(3.2))",
            "call println(i32(3.7))",
            "call println(i32(-3.7))",

            // f64 -> i64
            "call println(i64(3.2))",
            "call println(i64(3.7))",
            "call println(i64(-3.7))",

            // i32 -> i64
            "call println(i64(i32(99)))",

            // i64 -> i32
            "call println(i32(5))",
            "call println(i32(-5))",
            "call println(i32($maxIntPlusOne))",
            "call println(i32($minIntMinusOne))",
        )
        // Compare casting result with Kotlin
        val maxIntPlusOneAsInt : Int = maxIntPlusOne.toInt()
        val minIntMinusOneAsInt : Int = minIntMinusOne.toInt()

        val sourceFile = createSourceFile(source, COL)
        compileLlvmAndAssertSuccess(sourceFile)
        runLlvmAndAssertSuccess(listOf(), listOf(
            // f64 -> i32
            "3",
            "3",
            "-3",

            // f64 -> i64
            "3",
            "3",
            "-3",

            // i32 -> i64
            "99",

            // i64 -> i32
            "5",
            "-5",
            "$maxIntPlusOneAsInt",
            "$minIntMinusOneAsInt",
        ))
    }

    @Test
    fun shouldCastToFloat() {
        val source = listOf(
            // i32 -> f64
            "call println(f64(i32(15)))",
            "call println(f64(i32(-15)))",

            // i64 -> f64
            "call println(f64(3))",
            "call println(f64(-3))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(listOf(), listOf(
            // i32 -> f64
            "15.000000",
            "-15.000000",

            // i64 -> f64
            "3.000000",
            "-3.000000",
        ))
    }
}
