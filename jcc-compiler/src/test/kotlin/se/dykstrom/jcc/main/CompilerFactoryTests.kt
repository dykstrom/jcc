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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.common.error.CompilationErrorListener
import java.nio.file.Path

class CompilerFactoryTests {

    private val errorListener = CompilationErrorListener()

    private val factory = CompilerFactory.builder().errorListener(errorListener).build()

    @Test
    fun shouldCreateAssembunnyCompiler() {
        // Given
        val sourcePath = Path.of("foo.asmb")

        // When
        val compiler = factory.create("", sourcePath, null)

        // Then
        assertEquals(sourcePath, compiler.sourcePath())
    }

    @Test
    fun shouldCreateColCompiler() {
        // Given
        val sourcePath = Path.of("foo.col")

        // When
        val compiler = factory.create("", sourcePath, null)

        // Then
        assertEquals(sourcePath, compiler.sourcePath())
    }

    @Test
    fun shouldCreateColLlvmCompiler() {
        // Given
        val sourcePath = Path.of("foo.col")
        val factory = CompilerFactory.builder().backend(Backend.LLVM).errorListener(errorListener).build()

        // When
        val compiler = factory.create("", sourcePath, null)

        // Then
        assertEquals(sourcePath, compiler.sourcePath())
        assertNull(compiler.outputPath())
    }

    @Test
    fun shouldCreateTinyCompiler() {
        // Given
        val sourcePath = Path.of("foo.tiny")
        val outputPath = Path.of("foo.exe")

        // When
        val compiler = factory.create("", sourcePath, null)

        // Then
        assertEquals(sourcePath, compiler.sourcePath())
        assertEquals(outputPath, compiler.outputPath())
    }

    @Test
    fun shouldCreateTinyLlvmCompiler() {
        // Given
        val sourcePath = Path.of("foo.tiny")
        val factory = CompilerFactory.builder().backend(Backend.LLVM).errorListener(errorListener).build()

        // When
        val compiler = factory.create("", sourcePath, null)

        // Then
        assertEquals(sourcePath, compiler.sourcePath())
        assertNull(compiler.outputPath())
    }

    @Test
    fun shouldCreateTinyLlvmCompilerWithOutputFilename() {
        // Given
        val sourcePath = Path.of("foo.tiny")
        val outputPath = Path.of("foo")
        val factory = CompilerFactory.builder().backend(Backend.LLVM).errorListener(errorListener).build()

        // When
        val compiler = factory.create("", sourcePath, outputPath)

        // Then
        assertEquals(sourcePath, compiler.sourcePath())
        assertEquals(outputPath, compiler.outputPath())
    }

    @Test
    fun shouldCreateBasicCompilerWithOutputFilename() {
        // Given
        val sourcePath = Path.of("foo.bas")
        val outputPath = Path.of("bar.exe")

        // When
        val compiler = factory.create("", sourcePath, outputPath)

        // Then
        assertEquals(sourcePath, compiler.sourcePath())
        assertEquals(outputPath, compiler.outputPath())
    }

    @Test
    fun shouldNotCreateCompilerForUnknownLanguage() {
        // Given
        val sourcePath = Path.of("foo.cpp")

        // When
        val exception = assertThrows<IllegalArgumentException> { factory.create("", sourcePath, null) }

        // Then
        assertTrue(exception.message!!.contains("foo.cpp"))
    }

    @Test
    fun shouldNotCreateCompilerForNoLanguage() {
        // Given
        val sourcePath = Path.of("foo")

        // When
        val exception = assertThrows<IllegalArgumentException> { factory.create("", sourcePath, null) }

        // Then
        assertTrue(exception.message!!.contains("foo"))
    }
}
