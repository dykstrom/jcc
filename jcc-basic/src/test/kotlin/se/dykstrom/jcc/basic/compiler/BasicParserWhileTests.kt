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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BasicParserWhileTests : AbstractBasicParserTests() {

    @Test
    fun shouldParseEmptyWhile() {
        parse("10 while -1 20 wend")
        parse("while -1 wend")
    }

    @Test
    fun shouldParseWhile() {
        parse("10 while -1 20 print -1 30 wend")
        parse("""
            while -1
              a = 5
              b = a + 1
              print a; b
            wend
        """)
    }

    @Test
    fun shouldNotParseWhileWithoutExpression() {
        assertThrows<IllegalStateException> { parse("while print 1 wend") }
    }

    @Test
    fun shouldNotParseWhileWithoutWend() {
        assertThrows<IllegalStateException> { parse("10 while -1 20 print 1") }
    }
}
