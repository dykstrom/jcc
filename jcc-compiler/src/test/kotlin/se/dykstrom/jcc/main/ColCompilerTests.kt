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
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.error.SemanticsException
import se.dykstrom.jcc.common.error.SyntaxException
import java.nio.file.Files
import java.nio.file.Path

class ColCompilerTests {

    private val sourcePath = Path.of("file.col")
    private val outputPath = Path.of("file.ll")
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
        val compiler = factory.create("call println(17)", sourcePath, outputPath)

        // When
        val text = compiler.compile().toText()

        // Then
        assertTrue(errorListener.errors.isEmpty())
        assertTrue(text.contains("call i32 (ptr, ...) @printf(ptr @_.printf.fmt.I64.nl, i64 17)"), text)
    }

    @Test
    fun shouldCompileFunctionCallingOtherFunction() {
        // Given
        val compiler = factory.create("""
            call println(foo(5))
            fun foo(a as i64) -> i64 := bar(a)
            fun bar(b as i64) -> i64 := -b
            """, sourcePath, outputPath)

        // When
        val text = compiler.compile().toText()

        // Then
        assertTrue(errorListener.errors.isEmpty())
        assertTrue(text.contains("define tailcc i64 @foo_I64(i64 %0)"), text)
        assertTrue(text.contains("define tailcc i64 @bar_I64(i64 %0)"), text)
        assertTrue(text.contains("call tailcc i64 @foo_I64(i64 5)"), text)
        assertTrue(text.contains("call tailcc i64 @bar_I64(i64 %1)"), text)
    }

    @Test
    fun shouldFailWithSyntaxError() {
        val compiler = factory.create("alias foo := ", sourcePath, outputPath)
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
