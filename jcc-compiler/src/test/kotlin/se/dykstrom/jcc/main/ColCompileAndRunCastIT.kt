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
import se.dykstrom.jcc.main.Language.COL

/**
 * Compile-and-run integration tests for COL, specifically for testing casting.
 *
 * @author Johan Dykstrom
 */
class ColCompileAndRunCastIT : AbstractIntegrationTests() {

    @Test
    fun shouldCastToInt() {
        val maxIntPlusOne = Integer.MAX_VALUE.toLong() + 1L
        val minIntMinusOne = Integer.MIN_VALUE.toLong() - 1L

        val source = listOf(
            // f32 -> i32
            "call println(i32(f32(3.2)))",

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

            // i32, i64 -> i128 -> i64
            "call println(i64(i128(-7i32)))",
            "call println(i64(i128(9223372036854775807)))",

            // f32, f64 -> i128
            "call println(i64(i128(2.5f32)))",
            "call println(i64(i128(-3.7)))",
            "call println(i64(i128(1.0e20) div 10000000000i128))",

            // i128 -> i32, i64 keeps the low bits
            "call println(i32(170141183460469231731687303715884105727i128))",
            "call println(i64(18446744073709551617i128))",
            "call println(i64(i128(i128(17))))",
        )
        // Compare casting result with Kotlin
        val maxIntPlusOneAsInt : Int = maxIntPlusOne.toInt()
        val minIntMinusOneAsInt : Int = minIntMinusOne.toInt()

        val sourceFile = createSourceFile(source, COL)
        val expected = listOf(
            // f32 -> i32
            "3",

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

            // i32, i64 -> i128 -> i64
            "-7",
            "9223372036854775807",

            // f32, f64 -> i128
            "2",
            "-3",
            "10000000000",

            // i128 -> i32, i64
            "-1",
            "1",
            "17",
        )
        // A cast of a literal is folded at -O1
        listOf(arrayOf(), arrayOf("-O1")).forEach { args ->
            compileAndAssertSuccess(sourceFile, COL, *args)
            runAndAssertSuccess(listOf(), expected)
        }
    }

    @Test
    fun shouldCastToFloat() {
        val source = listOf(
            // f64 -> f32
            "call println(f32(20.0))",

            // i64 -> f32
            "call println(f32(20))",

            // f32 -> f64
            "call println(f64(f32(15)))",

            // i32 -> f64
            "call println(f64(i32(15)))",
            "call println(f64(i32(-15)))",

            // i64 -> f64
            "call println(f64(3))",
            "call println(f64(-3))",

            // i128 -> f32
            "call println(f32(1000i128))",

            // i128 -> f64
            "call println(f64(100000000000000000000i128))",
            "call println(f64(-1i128))",
        )
        val sourcePath = createSourceFile(source, COL)
        val expected = listOf(
            // f64 -> f32
            "20.000000",

            // i64 -> f32
            "20.000000",

            // f32 -> f64
            "15.000000",

            // i32 -> f64
            "15.000000",
            "-15.000000",

            // i64 -> f64
            "3.000000",
            "-3.000000",

            // i128 -> f32
            "1000.000000",

            // i128 -> f64
            "100000000000000000000.000000",
            "-1.000000",
        )
        // A cast of a literal is folded at -O1
        listOf(arrayOf(), arrayOf("-O1")).forEach { args ->
            compileAndAssertSuccess(sourcePath, COL, *args)
            runAndAssertSuccess(listOf(), expected)
        }
    }

    @Test
    fun implicitCasts() {
        val source = listOf(
            // i32 -> i64
            "call println(i32(5) + 7)",
            // f32 -> f64
            "call println(f32(3.0) + 8.0)",
        )

        val sourceFile = createSourceFile(source, COL)
        compileAndAssertSuccess(sourceFile, language = COL)
        runAndAssertSuccess(listOf(), listOf(
            // i32 -> i64
            "12",
            // f32 -> f64
            "11.000000",
        ))
    }
}
