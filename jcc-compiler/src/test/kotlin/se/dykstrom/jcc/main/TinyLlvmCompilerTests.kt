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

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.error.SemanticsException
import se.dykstrom.jcc.common.error.SyntaxException
import se.dykstrom.jcc.llvm.LlvmBuiltIns.FUN_PRINTF_STR_VAR
import se.dykstrom.jcc.llvm.operation.CallOperation
import java.nio.file.Files
import java.nio.file.Path

@Tag("LLVM")
class TinyLlvmCompilerTests {

    private val sourcePath = Path.of("file.tiny")
    private val outputPath = Path.of("file.ll")
    private val assemblyPath = Path.of("file.s")
    private val errorListener = CompilationErrorListener()

    private val factory = CompilerFactory.builder()
        .backend(Backend.LLVM)
        .compileOnly(true)
        .errorListener(errorListener)
        .build()

    @AfterEach
    fun tearDown() {
        Files.deleteIfExists(outputPath)
        Files.deleteIfExists(assemblyPath)
    }

    @Disabled
    @Test
    fun shouldCompileOk() {
        // Given
        val compiler = factory.create("BEGIN WRITE 1 END", sourcePath, null)

        // When
        val lines = compiler.compile().lines()

        // Then
        assertTrue(errorListener.errors.isEmpty())
        assertEquals(1, lines
            .filterIsInstance<CallOperation>()
            .map { code -> code.callee() }
            .count { callee -> callee == FUN_PRINTF_STR_VAR.externalName() })
    }

    @Test
    fun shouldFailWithSyntaxError() {
        val compiler = factory.create("BEGIN FOO END", sourcePath, outputPath)
        assertThrows<SyntaxException> { compiler.compile() }
        assertEquals(1, errorListener.errors.size)
    }

    @Test
    fun shouldFailWithSemanticsError() {
        val compiler = factory.create("BEGIN WRITE hello END", sourcePath, outputPath)
        assertThrows<SemanticsException> { compiler.compile() }
        assertEquals(1, errorListener.errors.size)
    }
}
