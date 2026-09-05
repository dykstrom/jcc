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

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.error.SyntaxException
import java.nio.file.Files
import java.nio.file.Path

class AssembunnyCompilerTests {

    private val sourcePath = Path.of("file.asmb")
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
        val compiler = factory.create("inc a cpy a d dec a jnz a -2", sourcePath, outputPath)

        // When
        val text = compiler.compile().toText()

        // Then
        assertTrue(errorListener.errors.isEmpty())
        // inc a
        assertTrue(text.contains("%1 = add i32 %0, 1"), text)
        // dec a
        assertTrue(text.contains("%4 = sub i32 %3, 1"), text)
        // jnz a -2
        assertTrue(text.contains("%6 = icmp eq i32 %5, 0"), text)
        assertTrue(text.contains("br i1 %6, label %L0, label %.line1"), text)
    }

    @Test
    fun shouldFailWithSyntaxErrorInc() {
        val compiler = factory.create("inc e", sourcePath, outputPath)
        assertThrows<SyntaxException> { compiler.compile() }
        assertEquals(2, errorListener.errors.size)
    }

    @Test
    fun shouldFailWithSyntaxErrorCpy() {
        val compiler = factory.create("cpy a 1", sourcePath, outputPath)
        assertThrows<SyntaxException> { compiler.compile() }
        assertEquals(1, errorListener.errors.size)
    }
}
