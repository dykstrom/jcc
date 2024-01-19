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

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.common.assembly.base.Label
import se.dykstrom.jcc.common.assembly.instruction.CallDirect
import se.dykstrom.jcc.common.assembly.instruction.CallIndirect
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.error.SemanticsException
import se.dykstrom.jcc.common.error.SyntaxException
import se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_PRINTF
import java.nio.file.Files
import java.nio.file.Path

class ColCompilerTests {

    private val sourcePath = Path.of("file.col")
    private val outputPath = Path.of("file.asm")
    private val errorListener = CompilationErrorListener()

    private val factory = CompilerFactory.builder()
        .compileOnly(true)
        .errorListener(errorListener)
        .build()

    @AfterEach
    fun tearDown() {
        Files.deleteIfExists(outputPath)
    }

    @Test
    fun shouldCompileOk() {
        // Given
        val compiler = factory.create("println 17", sourcePath, outputPath)

        // When
        val lines = compiler.compile().lines()

        // Then
        assertTrue(errorListener.errors.isEmpty())
        assertEquals(1, lines.filterIsInstance<CallIndirect>().count { it.target == "[" + FUN_PRINTF.mappedName + "]" })
    }

    @Test
    fun shouldCompileFunctionCallingOtherFunction() {
        // Given
        val compiler = factory.create("""
            println foo(5)
            fun foo(a as i64) -> i64 = bar(a)
            fun bar(b as i64) -> i64 = -b
            """, sourcePath, outputPath)

        // When
        val lines = compiler.compile().lines()

        // Then
        assertTrue(errorListener.errors.isEmpty())
        assertEquals(1, lines.filterIsInstance<CallDirect>().count { it.target == "__foo_I64" })
        assertEquals(1, lines.filterIsInstance<CallDirect>().count { it.target == "__bar_I64" })
        assertEquals(1, lines.filterIsInstance<Label>().count { it.mappedName == "__foo_I64" })
        assertEquals(1, lines.filterIsInstance<Label>().count { it.mappedName == "__bar_I64" })
    }

    @Test
    fun shouldFailWithSyntaxError() {
        val compiler = factory.create("alias foo = ", sourcePath, outputPath)
        assertThrows<SyntaxException> { compiler.compile() }
        assertEquals(1, errorListener.errors.size)
    }

    @Test
    fun shouldFailWithSemanticsError() {
        val compiler = factory.create("alias foo as bar", sourcePath, outputPath)
        assertThrows<SemanticsException> { compiler.compile() }
        assertEquals(1, errorListener.errors.size)
    }
}
