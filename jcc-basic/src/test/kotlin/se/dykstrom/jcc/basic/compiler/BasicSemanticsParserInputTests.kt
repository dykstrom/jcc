/*
 * Copyright (C) 2019 Johan Dykstrom
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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests class `BasicSemanticsParser`, especially functionality related to (LINE) INPUT statements.
 *
 * @author Johan Dykstrom
 * @see BasicSemanticsParser
 */
class BasicSemanticsParserInputTests : AbstractBasicSemanticsParserTests() {

    @Test
    fun shouldParseLineInputWithString() {
        parse("line input foo$")
        parse("line input; bar$")
        parse("dim axe as string : line input; \"prompt\"; axe")
    }

    @Test
    fun shouldNotParseLineInputWithInvalidType() {
        parseAndExpectException("line input a%", "type string, not integer")
        parseAndExpectException("line input b#", "type string, not double")
        parseAndExpectException("line input c", "type string, not double")
    }

    @Test
    fun shouldNotParseLineInputWithConstant() {
        parseAndExpectException("const foo = \"moo\" : line input foo", "cannot use constant 'foo'")
    }

    @Test
    fun shouldWarnAboutUndefinedVariableAtTheLineInput() {
        // LINE INPUT is where the variable first appears, so that is where it is defined and
        // where an undefined one is warned about, not on the statement that reads it afterwards.
        parse("LINE INPUT \"name: \"; a$\nPRINT a$\n")
        assertEquals(1, errorListener.warnings.size, errorListener.warnings.toString())
        val warning = errorListener.warnings[0]
        assertEquals("undefined variable: a\$", warning.msg)
        assertEquals(1, warning.line)
        // The caret points at the variable, as it does for an assignment
        assertEquals(21, warning.column)
    }

    @Test
    fun shouldNotWarnAboutDeclaredVariableInLineInput() {
        parse("DIM a AS STRING\nLINE INPUT \"name: \"; a\nPRINT a\n")
        assertTrue(errorListener.warnings.isEmpty(), errorListener.warnings.toString())
    }
}
