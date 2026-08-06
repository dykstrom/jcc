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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.antlr4.Antlr4Utils
import se.dykstrom.jcc.tiny.compiler.TinyTests.Companion.SYNTAX_ERROR_LISTENER

/**
 * Tests the class `TinyParser` that is generated from the Tiny grammar.
 *
 * @author Johan Dykstrom
 */
class TinyParserTests {

    @Test
    fun testWrite() {
        parse("BEGIN WRITE 17 END")
    }

    @Test
    fun testReadWrite() {
        parse("BEGIN READ n WRITE n END")
    }

    @Test
    fun testAssignment() {
        parse("BEGIN a := 0 END")
    }

    @Test
    fun testReadAssignWrite() {
        parse("BEGIN READ a b := a + 1 WRITE b END")
    }

    @Test
    fun testMultipleArgs() {
        parse("BEGIN READ a, b c := a + b WRITE a, b, c END")
    }

    @Test
    fun testMultipleAssignments() {
        parse(
            """
            |BEGIN
            |  READ a
            |  b := a + 1
            |  c := b - 1
            |  WRITE a, b, c
            |END
            |""".trimMargin()
        )
    }

    @Test
    fun testNegativeNumber() {
        parse("BEGIN a := -3 WRITE a END")
    }

    private fun parse(text: String) {
        val lexer = TinyLexer(CharStreams.fromString(text))
        lexer.removeErrorListeners()
        lexer.addErrorListener(SYNTAX_ERROR_LISTENER)
        val parser = TinyParser(CommonTokenStream(lexer))
        parser.removeErrorListeners()
        parser.addErrorListener(SYNTAX_ERROR_LISTENER)
        val ctx = parser.program()
        Antlr4Utils.checkParsingComplete(parser)
        assertNotNull(ctx)
    }
}
