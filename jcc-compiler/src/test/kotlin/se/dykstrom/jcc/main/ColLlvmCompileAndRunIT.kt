/*
 * Copyright (C) 2025 Johan Dykstrom
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
 * Compile-and-run integration tests for the COL LLVM backend.
 *
 * @author Johan Dykstrom
 */
@Tag("LLVM")
class ColLlvmCompileAndRunIT : AbstractIntegrationTests() {

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
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "7",
                "-7",
                "5.300000",
                "-5.300000",
                "1",
                "0",
            ),
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
            "call println(24 == 24)",
            "call println(2.345 == 2.345)",
            "call println(0 != 1)",
            "call println(0 < 1)",
            "call println(0 <= 1)",
            "call println(0 > 1)",
            "call println(0 >= 1)",
            "call println(1.0 >= 1.0)",
            "call println(true and false)",
            "call println(false and true)",
            "call println(0 > -1 and -1 > -2)",
            "call println(true or false)",
            "call println(false or true)",
            "call println(false xor false)",
            "call println(false xor 1 != 0)",
            "call println(not false)",
            "call println(not (1.0 > 0.5))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(listOf(), listOf(
            // Arithmetic operators
            "6",
            "-6",
            "9000",
            "2",
            "254",
            "0.990000",
            "1000000000.000000",
            "6",
            "5.000000",
            "3",
            "1",
            "-80",
            // Bitwise operators
            "2",
            "7",
            "5",
            "-1",
            // Relational operators
            "0",
            "1",
            "1",
            "1",
            "1",
            "1",
            "0",
            "0",
            "1",
            // Logical operators
            "0",
            "0",
            "1",
            "1",
            "1",
            "0",
            "1",
            "1",
            "0",
        ))
    }

    @Test
    fun shouldCallIntrinsicFunctions() {
        val source = listOf(
            // Rounding
            "call println(ceil(3.7))",
            "call println(ceil(f32(3.7)))",
            "call println(floor(3.7))",
            "call println(floor(f32(3.7)))",
            "call println(round(3.7))",
            "call println(round(f32(3.7)))",
            "call println(round(-3.7))",
            "call println(round(f32(-3.7)))",
            "call println(trunc(3.7))",
            "call println(trunc(f32(3.7)))",
            "call println(trunc(-3.7))",
            "call println(trunc(f32(-3.7)))",
            // Math
            "call println(sqrt(4.0))",
            "call println(sqrt(f32(4.0)))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                // Rounding
                "4.000000",
                "4.000000",
                "3.000000",
                "3.000000",
                "4.000000",
                "4.000000",
                "-4.000000",
                "-4.000000",
                "3.000000",
                "3.000000",
                "-3.000000",
                "-3.000000",
                // Math
                "2.000000",
                "2.000000",
            ),
        )
    }
}
