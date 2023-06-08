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
package se.dykstrom.jcc.basic.compiler

import org.antlr.v4.runtime.CharStreams
import org.junit.Before
import org.junit.Test
import se.dykstrom.jcc.common.assembly.instruction.CallIndirect
import se.dykstrom.jcc.common.assembly.instruction.Jmp
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.functions.BuiltInFunctions
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BasicCompilerTests {
    
    private val errorListener = CompilationErrorListener()
    
    private val compiler = BasicCompiler()
    
    @Before
    fun setUp() {
        compiler.sourceFilename = FILENAME
        compiler.errorListener = errorListener
    }

    @Test
    fun testCompile_Ok() {
        compiler.inputStream = CharStreams.fromString("10 PRINT \"Hi!\"\n20 GOTO 10")
        val result = compiler.compile()
        assertTrue(errorListener.errors.isEmpty())
        assertEquals(1, result.lines()
            .filterIsInstance<CallIndirect>()
            .map { code -> code.target }
            .count { target -> target == "[" + BuiltInFunctions.FUN_PRINTF.mappedName + "]" })
        assertEquals(1, result.lines()
            .filterIsInstance<Jmp>()
            .map { code -> code.target.mappedName }
            .count { target -> target == "__line_10" })
    }

    @Test
    fun testCompile_SyntaxErrorGoto() {
        compiler.inputStream = CharStreams.fromString("10 GOTO")
        assertNull(compiler.compile())
        assertEquals(1, errorListener.errors.size)
    }

    @Test
    fun testCompile_SyntaxErrorAssignment() {
        compiler.inputStream = CharStreams.fromString("10 LET = 7")
        assertNull(compiler.compile())
        assertEquals(1, errorListener.errors.size)
    }

    @Test
    fun testCompile_SemanticsErrorGoto() {
        compiler.inputStream = CharStreams.fromString("10 GOTO 20")
        assertNull(compiler.compile())
        assertEquals(1, errorListener.errors.size)
    }

    @Test
    fun testCompile_SemanticsErrorAssignment() {
        compiler.inputStream = CharStreams.fromString("10 LET A$ = 17\n20 LET A% = \"B\"")
        assertNull(compiler.compile())
        assertEquals(2, errorListener.errors.size)
    }

    companion object {
        private const val FILENAME = "file.bas"
    }
}
