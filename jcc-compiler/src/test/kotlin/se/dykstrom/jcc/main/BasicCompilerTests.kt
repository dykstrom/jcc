/*
 * Copyright (C) 2016 Johan Dykstrom
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
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.common.assembly.instruction.CallIndirect
import se.dykstrom.jcc.common.assembly.instruction.Jmp
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.error.SemanticsException
import se.dykstrom.jcc.common.error.SyntaxException
import se.dykstrom.jcc.common.functions.BuiltInFunctions
import java.nio.file.Files
import java.nio.file.Path

class BasicCompilerTests {

    private val sourcePath = Path.of("file.bas")
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
        val compiler = factory.create("10 PRINT \"Hi!\" 20 GOTO 10", sourcePath, outputPath)

        // When
        val lines = compiler.compile().lines()

        // Then
        assertTrue(errorListener.errors.isEmpty())
        assertEquals(1, lines
            .filterIsInstance<CallIndirect>()
            .map { code -> code.target }
            .count { target -> target == "[" + BuiltInFunctions.FUN_PRINTF.mappedName + "]" })
        assertEquals(1, lines
            .filterIsInstance<Jmp>()
            .map { code -> code.target.mappedName }
            .count { target -> target == "__line_10" })
    }

    @Test
    fun shouldFailWithSyntaxErrorGoto() {
        val compiler = factory.create("10 GOTO", sourcePath, outputPath)
        assertThrows<SyntaxException> { compiler.compile() }
        assertEquals(1, errorListener.errors.size)
    }

    @Test
    fun shouldFailWithSyntaxErrorAssignment() {
        val compiler = factory.create("10 LET = 7", sourcePath, outputPath)
        assertThrows<SyntaxException> { compiler.compile() }
        assertEquals(1, errorListener.errors.size)
    }

    @Test
    fun shouldFailWithSemanticsErrorGoto() {
        val compiler = factory.create("10 GOTO 20", sourcePath, outputPath)
        assertThrows<SemanticsException> { compiler.compile() }
        assertEquals(1, errorListener.errors.size)
    }

    @Test
    fun shouldFailWithSemanticsErrorAssignment() {
        val compiler = factory.create("10 LET A$ = 17 20 LET A% = \"B\"", sourcePath, outputPath)
        assertThrows<SemanticsException> { compiler.compile() }
        assertEquals(2, errorListener.errors.size)
    }
}
