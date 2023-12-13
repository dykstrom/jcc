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

package se.dykstrom.jcc.assembunny.compiler

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.Assert.assertThrows
import org.junit.Test
import se.dykstrom.jcc.antlr4.Antlr4Utils
import se.dykstrom.jcc.assembunny.compiler.AssembunnyTests.Companion.ERROR_LISTENER

class AssembunnyParserTests {

    @Test
    fun shouldParseEmptyProgram() {
        parse("")
    }

    @Test
    fun shouldParseSingleStatement() {
        parse("inc a")
        parse("dec b")
        parse("cpy a b")
        parse("cpy 1 c")
        parse("jnz d 0")
        parse("jnz a -1")
        parse("outn a")
    }

    @Test
    fun shouldParseMultipleStatements() {
        parse("inc a dec a cpy c d jnz b -1 outn b")
    }

    @Test
    fun shouldNotParseMissingRegister() {
        assertThrows(IllegalStateException::class.java) { parse("inc") }
    }

    @Test
    fun shouldNotParseInvalidRegister() {
        assertThrows(IllegalStateException::class.java) { parse("inc e") }
    }

    @Test
    fun shouldNotParseCopyToInteger() {
        assertThrows(IllegalStateException::class.java) { parse("cpy a 7") }
    }

    /**
     * Parses the given program text.
     */
    private fun parse(text: String) {
        val lexer = AssembunnyLexer(CharStreams.fromString(text))
        lexer.addErrorListener(ERROR_LISTENER)
        val parser = AssembunnyParser(CommonTokenStream(lexer))
        parser.addErrorListener(ERROR_LISTENER)
        parser.program()
        Antlr4Utils.checkParsingComplete(parser)
    }
}
