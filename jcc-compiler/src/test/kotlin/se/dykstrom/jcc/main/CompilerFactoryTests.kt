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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import se.dykstrom.jcc.common.error.CompilationErrorListener
import java.nio.file.Path
import kotlin.test.assertTrue

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
        assertEquals(sourcePath, compiler.sourcePath)
    }

    @Test
    fun shouldCreateColCompiler() {
        // Given
        val sourcePath = Path.of("foo.col")

        // When
        val compiler = factory.create("", sourcePath, null)

        // Then
        assertEquals(sourcePath, compiler.sourcePath)
    }

    @Test
    fun shouldCreateTinyCompiler() {
        // Given
        val sourcePath = Path.of("foo.tiny")

        // When
        val compiler = factory.create("", sourcePath, null)

        // Then
        assertEquals(sourcePath, compiler.sourcePath)
    }

    @Test
    fun shouldCreateBasicCompilerWithOutputFilename() {
        // Given
        val sourcePath = Path.of("foo.bas")

        // When
        val compiler = factory.create("", sourcePath, Path.of("bar.exe"))

        // Then
        assertEquals(sourcePath, compiler.sourcePath)
    }

    @Test
    fun shouldNotCreateCompilerForUnknownLanguage() {
        // Given
        val sourcePath = Path.of("foo.cpp")

        // When
        val exception = assertThrows(IllegalArgumentException::class.java) { factory.create("", sourcePath, null) }

        // Then
        assertTrue(exception.message!!.contains("foo.cpp"))
    }

    @Test
    fun shouldNotCreateCompilerForNoLanguage() {
        // Given
        val sourcePath = Path.of("foo")

        // When
        val exception = assertThrows(IllegalArgumentException::class.java) { factory.create("", sourcePath, null) }

        // Then
        assertTrue(exception.message!!.contains("foo"))
    }
}
