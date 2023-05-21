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

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.Assert.assertThrows
import org.junit.Test
import se.dykstrom.jcc.common.ast.Program
import se.dykstrom.jcc.common.error.InvalidException
import se.dykstrom.jcc.common.error.UndefinedException
import se.dykstrom.jcc.common.utils.FormatUtils.EOL
import se.dykstrom.jcc.common.utils.ParseUtils
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.NAME_A
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.NAME_B
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.NAME_C
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.NAME_N
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.NAME_UNDEFINED
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.SEMANTICS_ERROR_LISTENER
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.SYNTAX_ERROR_LISTENER
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TinySemanticsParserTests {

    private val semanticsParser = TinySemanticsParser()

    @Test
    fun testWrite() {
        parse("BEGIN WRITE 17 END")
        val symbols = semanticsParser.symbols
        assertEquals(0, symbols.size())
    }

    @Test
    fun testReadWrite() {
        parse("BEGIN" + EOL + "READ n" + EOL + "WRITE n" + EOL + "END")
        val symbols = semanticsParser.symbols
        assertEquals(1, symbols.size())
        assertTrue(symbols.contains(NAME_N))
    }

    @Test
    fun testAssignment() {
        parse("BEGIN" + EOL + "a := 0" + EOL + "END")
        val symbols = semanticsParser.symbols
        assertEquals(1, symbols.size())
        assertTrue(symbols.contains(NAME_A))
    }

    @Test
    fun testReadAssignWrite() {
        parse("BEGIN" + EOL + "READ a" + EOL + "b := a + 1" + EOL + "WRITE b" + EOL + "END")
        val symbols = semanticsParser.symbols
        assertEquals(2, symbols.size())
        assertTrue(symbols.contains(NAME_A, NAME_B))
    }

    @Test
    fun testMultipleArgs() {
        parse("BEGIN" + EOL + "READ a, b" + EOL + "c := a + b" + EOL + "WRITE a, b, c" + EOL + "END")
        val symbols = semanticsParser.symbols
        assertEquals(3, symbols.size())
        assertTrue(symbols.contains(NAME_A, NAME_B, NAME_C))
    }

    @Test
    fun testMultipleAssignments() {
        parse("""
            |BEGIN
            |  READ a
            |  b := a + 1
            |  c := b - 1
            |  WRITE a, b, c
            |END
            |""".trimMargin()
        )
        val symbols = semanticsParser.symbols
        assertEquals(3, symbols.size())
        assertTrue(symbols.contains(NAME_A, NAME_B, NAME_C))
    }

    @Test
    fun testMaxI64() {
        parse("BEGIN WRITE 9223372036854775807 END")
        val symbols = semanticsParser.symbols
        assertEquals(0, symbols.size())
    }

    /**
     * Invalid integer -> overflow.
     */
    @Test
    fun testOverflowI64() {
        val value = "9223372036854775808"
        val e = assertThrows(IllegalStateException::class.java) { parse("BEGIN WRITE $value END") }
        val ie = e.cause as InvalidException
        assertEquals(value, ie.value)
    }

    /**
     * Undefined identifier in write statement.
     */
    @Test
    fun testUndefinedInWrite() {
        val e = assertThrows(IllegalStateException::class.java) { parse("BEGIN WRITE undefined END") }
        val ue = e.cause as UndefinedException
        assertEquals(NAME_UNDEFINED, ue.name)
    }

    /**
     * Undefined identifier in assign statement.
     */
    @Test
    fun testUndefinedInAssign() {
        val e = assertThrows(IllegalStateException::class.java) { parse("BEGIN a := undefined END") }
        val ue = e.cause as UndefinedException
        assertEquals(NAME_UNDEFINED, ue.name)
    }

    /**
     * Undefined identifier in complex expression.
     */
    @Test
    fun testUndefinedInExpression() {
        val e = assertThrows(IllegalStateException::class.java) { parse("BEGIN WRITE 1 + undefined - 2 END") }
        val ue = e.cause as UndefinedException
        assertEquals(NAME_UNDEFINED, ue.name)
    }

    /**
     * Undefined identifier in expression list.
     */
    @Test
    fun testUndefinedInList() {
        val e = assertThrows(IllegalStateException::class.java) { parse("BEGIN WRITE 1, undefined, 3 END") }
        val ue = e.cause as UndefinedException
        assertEquals(NAME_UNDEFINED, ue.name)
    }

    private fun parse(text: String) {
        val lexer = TinyLexer(CharStreams.fromString(text))
        lexer.addErrorListener(SYNTAX_ERROR_LISTENER)
        val parser = TinyParser(CommonTokenStream(lexer))
        parser.addErrorListener(SYNTAX_ERROR_LISTENER)
        val ctx = parser.program()
        ParseUtils.checkParsingComplete(parser)
        val visitor = TinySyntaxVisitor()
        val program = visitor.visitProgram(ctx) as Program
        semanticsParser.addErrorListener(SEMANTICS_ERROR_LISTENER)
        semanticsParser.program(program)
    }
}
