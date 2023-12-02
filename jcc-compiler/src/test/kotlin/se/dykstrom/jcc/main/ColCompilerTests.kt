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

import org.junit.Assert.assertThrows
import org.junit.Test
import se.dykstrom.jcc.common.assembly.instruction.CallIndirect
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.error.SemanticsException
import se.dykstrom.jcc.common.error.SyntaxException
import se.dykstrom.jcc.common.functions.BuiltInFunctions
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColCompilerTests {

    private val sourcePath = Path.of("file.col")
    private val outputPath = Path.of("file.asm")
    private val errorListener = CompilationErrorListener()

    private val factory = CompilerFactory.builder()
        .compileOnly(true)
        .errorListener(errorListener)
        .build()

    @AfterTest
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
        assertEquals(1, lines
            .filterIsInstance<CallIndirect>()
            .map { code -> code.target }
            .count { target -> target == "[" + BuiltInFunctions.FUN_PRINTF.mappedName + "]" })
    }

    @Test
    fun shouldFailWithSyntaxError() {
        val compiler = factory.create("alias foo = ", sourcePath, outputPath)
        assertThrows(SyntaxException::class.java) { compiler.compile() }
        assertEquals(1, errorListener.errors.size)
    }

    @Test
    fun shouldFailWithSemanticsError() {
        val compiler = factory.create("alias foo = bar", sourcePath, outputPath)
        assertThrows(SemanticsException::class.java) { compiler.compile() }
        assertEquals(1, errorListener.errors.size)
    }
}
