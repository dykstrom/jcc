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
 * Compile-and-run integration tests for BASIC, focusing on programs
 * compiled with optimization enabled.
 *
 * @author Johan Dykstrom
 */
@Tag("LLVM")
class BasicCompileAndRunOptimizationIT : AbstractIntegrationTests() {

    @Test
    fun shouldIncrementAndDecrement() {
        val source = listOf(
            "a% = 5",
            "a% = a% + 1",
            "PRINT a%",
            "b% = 5",
            "b% = b% - 1",
            "PRINT b%",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC, "-O1")
        runAndAssertSuccess(listOf(), listOf("6", "4"))
    }

    @Test
    fun shouldAddAssignAndSubAssign() {
        val source = listOf(
            "a% = 5",
            "a% = a% + 3",
            "PRINT a%",
            "b% = 5",
            "b% = b% - 3",
            "PRINT b%",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC, "-O1")
        runAndAssertSuccess(listOf(), listOf("8", "2"))
    }

    @Test
    fun shouldMulAssign() {
        val source = listOf(
            "a% = 5",
            "a% = a% * 3",
            "PRINT a%",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC, "-O1")
        runAndAssertSuccess(listOf(), listOf("15"))
    }

    @Test
    fun shouldIDivAssign() {
        val source = listOf(
            "a% = 100",
            "a% = a% \\ 4",
            "PRINT a%",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC, "-O1")
        runAndAssertSuccess(listOf(), listOf("25"))
    }

    @Test
    fun shouldMulWithPowerOfTwo() {
        val source = listOf(
            "a% = 7",
            "b% = a% * 4",
            "PRINT b%",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC, "-O1")
        runAndAssertSuccess(listOf(), listOf("28"))
    }

    @Test
    fun shouldOptimizeFirstUseOfVariable() {
        // The optimized statement is the first use of the variable,
        // so the code generator must define the variable as well
        val source = listOf(
            "a% = a% + 1",
            "PRINT a%",
            "b% = b% * 3",
            "PRINT b%",
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC, "-O1")
        runAndAssertSuccess(listOf(), listOf("1", "0"))
    }
}
