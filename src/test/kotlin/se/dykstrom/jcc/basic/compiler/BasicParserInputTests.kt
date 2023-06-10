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

import org.junit.Test

class BasicParserInputTests : AbstractBasicParserTests() {

    @Test
    fun shouldParseLineInput() {
        parse("line input foo")
        parse("Line Input str$")
        parse("LINE INPUT a.string.variable")
        parse("line input; with.semicolon")
    }

    @Test
    fun shouldParseLineInputWithPrompt() {
        parse("line input \"prompt\"; variable")
        parse("Line Input \"\"; str$")
        parse("LINE INPUT; \"boo\"; foo")
    }

    @Test(expected = IllegalStateException::class)
    fun shouldRejectMissingInput() {
        parse("line foo")
    }

    @Test(expected = IllegalStateException::class)
    fun shouldRejectMissingVariable() {
        parse("line input;")
    }

    @Test(expected = IllegalStateException::class)
    fun shouldRejectMissingSemiColonAfterPrompt() {
        parse("line input \"prompt\" foo")
    }
}
