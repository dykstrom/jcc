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
import se.dykstrom.jcc.main.Language.TINY

/**
 * Compile-and-run integration tests for the Tiny LLVM backend.
 *
 * @author Johan Dykstrom
 */
@Tag("LLVM")
class TinyLlvmCompileAndRunIT : AbstractIntegrationTests() {

    @Test
    fun shouldWriteExpression() {
        val source = listOf("BEGIN WRITE 1 + 2 - 3 END")
        val sourcePath = createSourceFile(source, TINY)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(listOf(), listOf("0"))
    }

    @Test
    fun shouldReadAndWrite() {
        val source = listOf(
                "BEGIN",
                "  READ a",
                "  a := a + 1",
                "  WRITE a",
                "END"
        )
        val sourcePath = createSourceFile(source, TINY)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(listOf("5"), listOf("6"))
    }

    @Test
    fun shouldCalculateSum() {
        val source = listOf(
                "BEGIN",
                "  READ a, b",
                "  c := a + b",
                "  WRITE a, b, c",
                "END"
        )
        val sourcePath = createSourceFile(source, TINY)
        compileLlvmAndAssertSuccess(sourcePath)
        runLlvmAndAssertSuccess(listOf("17", "7"), listOf("17", "7", "24"))
    }

    @Test
    fun shouldOptimizeAddAndSub() {
        val source = listOf(
            "BEGIN",
            "  READ a",
            "  a := a + 1",
            "  WRITE a",
            "  a := a - 1",
            "  WRITE a",
            "  a := a + 5",
            "  WRITE a",
            "  a := a - 3",
            "  WRITE a",
            "END"
        )
        val sourceFile = createSourceFile(source, TINY)
        compileLlvmAndAssertSuccess(sourceFile, "-O1")
        runLlvmAndAssertSuccess(listOf("0"), listOf("1", "0", "5", "2"))
    }
}
