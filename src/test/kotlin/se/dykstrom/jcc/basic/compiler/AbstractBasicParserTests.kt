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

import org.antlr.v4.runtime.*
import se.dykstrom.jcc.common.utils.ParseUtils
import kotlin.test.assertNotNull

abstract class AbstractBasicParserTests {
    /**
     * Parses the given program text.
     */
    fun parse(text: String) {
        val lexer = BasicLexer(CharStreams.fromString(text))
        lexer.addErrorListener(SYNTAX_ERROR_LISTENER)

        val syntaxParser = BasicParser(CommonTokenStream(lexer))
        syntaxParser.addErrorListener(SYNTAX_ERROR_LISTENER)

        val ctx = syntaxParser.program()
        ParseUtils.checkParsingComplete(syntaxParser)
        assertNotNull(ctx)
    }

    companion object {
        private val SYNTAX_ERROR_LISTENER = object : BaseErrorListener() {
            override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException?) {
                throw IllegalStateException("Syntax error at $line:$charPositionInLine: $msg", e)
            }
        }
    }
}