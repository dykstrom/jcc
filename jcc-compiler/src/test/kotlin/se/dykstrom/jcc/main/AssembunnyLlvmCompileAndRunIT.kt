/*
 * Copyright (C) 2017 Johan Dykstrom
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
import se.dykstrom.jcc.main.Language.ASSEMBUNNY

/**
 * Compile-and-run integration tests for the Assembunny LLVM backend.
 *
 * @author Johan Dykstrom
 */
@Tag("LLVM")
class AssembunnyLlvmCompileAndRunIT : AbstractIntegrationTests() {

    @Test
    fun shouldExitWith5() {
        val source = listOf(
                "cpy 4 c",
                "cpy c a",
                "inc a",
                "dec a",
                "jnz a 2",
                "cpy 0 a",
                "inc a",
                "outn a"
        )
        val sourceFile = createSourceFile(source, ASSEMBUNNY)
        compileLlvmAndAssertSuccess(sourceFile)
        runLlvmAndAssertSuccess(listOf(), listOf("5"), 5)
    }

    @Test
    fun shouldExitWith12() {
        val source = listOf(
                "cpy 3 b",
                "cpy 4 c",
                "inc a",
                "dec c",
                "jnz c -2",
                "dec b",
                "jnz b -5",
                "outn a"
        )
        val sourceFile = createSourceFile(source, ASSEMBUNNY)
        compileLlvmAndAssertSuccess(sourceFile)
        runLlvmAndAssertSuccess(listOf(), listOf("12"), 12)
    }
}
