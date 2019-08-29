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

import org.junit.Test

class BasicParserWhileTests : AbstractBasicParserTest() {

    @Test
    fun shouldParseEmptyWhile() {
        parse("10 while true 20 wend")
        parse("while true wend")
    }

    @Test
    fun shouldParseWhile() {
        parse("10 while true 20 print true 30 wend")
        parse("""
            while true
              a = 5
              b = a + 1
              print a; b
            wend
        """)
    }

    @Test(expected = IllegalStateException::class)
    fun shouldNotParseWhileWithoutExpression() {
        parse("while print 1 wend")
    }

    @Test(expected = IllegalStateException::class)
    fun shouldNotParseWhileWithoutWend() {
        parse("10 while true 20 print 1")
    }
}
