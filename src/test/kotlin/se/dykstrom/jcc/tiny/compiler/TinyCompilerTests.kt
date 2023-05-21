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
package se.dykstrom.jcc.tiny.compiler

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.junit.Before
import org.junit.Test
import se.dykstrom.jcc.common.assembly.instruction.CallIndirect
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.functions.BuiltInFunctions
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TinyCompilerTests {

    private val errorListener = CompilationErrorListener()

    private val compiler = TinyCompiler()

    @Before
    fun setUp() {
        compiler.sourceFilename = FILENAME
        compiler.errorListener = errorListener
    }

    @Test
    fun shouldCompileOk() {
        val inputStream = CharStreams.fromString("BEGIN WRITE 1 END")
        compiler.inputStream = inputStream
        val result = compiler.compile()
        val lines = result.lines()
        assertTrue(errorListener.errors.isEmpty())
        assertEquals(1, lines
            .filterIsInstance<CallIndirect>()
            .map { code -> code.target }
            .count { target -> target == "[" + BuiltInFunctions.FUN_PRINTF.mappedName + "]" })
    }

    @Test
    fun shouldFailWithSyntaxError() {
        val inputStream: CharStream = CharStreams.fromString("BEGIN FOO END")
        compiler.inputStream = inputStream
        assertNull(compiler.compile())
        assertEquals(1, errorListener.errors.size)
    }

    @Test
    fun shouldFailWithSemanticsError() {
        val inputStream: CharStream = CharStreams.fromString("BEGIN WRITE hello END")
        compiler.inputStream = inputStream
        assertNull(compiler.compile())
        assertEquals(1, errorListener.errors.size)
    }

    companion object {
        private const val FILENAME = "file.tiny"
    }
}