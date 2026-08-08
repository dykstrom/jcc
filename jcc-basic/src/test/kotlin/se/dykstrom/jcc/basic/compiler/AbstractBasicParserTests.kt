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

import org.junit.jupiter.api.Assertions.assertNotNull
import se.dykstrom.jcc.antlr4.Antlr4Utils
import se.dykstrom.jcc.basic.BasicTests.Companion.ERROR_LISTENER
import se.dykstrom.jcc.basic.BasicTests.Companion.parseProgram
import se.dykstrom.jcc.common.error.CompilationError
import se.dykstrom.jcc.common.error.CompilationErrorListener

abstract class AbstractBasicParserTests {

    /**
     * Parses the given program text.
     */
    fun parse(text: String) {
        assertNotNull(parseProgram(text, ERROR_LISTENER))
    }

    /**
     * Parses the given program text, collecting every error instead of throwing on the first one,
     * and returns them in the order they were reported. Use this to assert on how many messages
     * one mistake produces; [parse] can only ever see the first.
     */
    fun parseCollectingErrors(text: String): List<CompilationError> {
        val errorListener = CompilationErrorListener()
        parseProgram(text, Antlr4Utils.asBaseErrorListener(errorListener))
        return errorListener.errors
    }
}
