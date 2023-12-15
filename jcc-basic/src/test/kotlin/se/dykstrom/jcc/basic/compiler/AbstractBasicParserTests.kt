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

package se.dykstrom.jcc.basic.compiler

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Assertions.assertNotNull
import se.dykstrom.jcc.antlr4.Antlr4Utils
import se.dykstrom.jcc.basic.BasicTests.Companion.ERROR_LISTENER

abstract class AbstractBasicParserTests {

    /**
     * Parses the given program text.
     */
    fun parse(text: String) {
        val lexer = BasicLexer(CharStreams.fromString(text))
        lexer.addErrorListener(ERROR_LISTENER)

        val syntaxParser = BasicParser(CommonTokenStream(lexer))
        syntaxParser.addErrorListener(ERROR_LISTENER)

        val ctx = syntaxParser.program()
        Antlr4Utils.checkParsingComplete(syntaxParser)
        assertNotNull(ctx)
    }
}
