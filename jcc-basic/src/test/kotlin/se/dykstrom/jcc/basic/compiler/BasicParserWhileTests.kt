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
        parse("""
            10 WHILE -1
            20 WEND
        """)
        parse("""
            WHILE -1
            WEND
        """)
    }

    @Test
    fun shouldParseWhile() {
        parse("""
            10 WHILE -1
            20 PRINT -1
            30 WEND
        """)
        parse("""
            WHILE -1
              a = 5
              b = a + 1
              PRINT a; b
            WEND
        """)
    }

    @Test
    fun shouldParseWhileWithCommentAfterExpression() {
        parse("""
            WHILE -1 ' loop forever
              PRINT 1
            WEND
        """)
    }

    @Test
    fun shouldNotParseWhileWithoutExpression() {
        assertThrows<IllegalStateException> {
            parse("""
                WHILE
                  PRINT 1
                WEND
            """)
        }
    }

    @Test
    fun shouldNotParseWhileWithoutWend() {
        assertThrows<IllegalStateException> {
            parse("""
                10 WHILE -1
                20 PRINT 1
            """)
        }
    }

    @Test
    fun shouldNotParseOneLineWhile() {
        assertThrows<IllegalStateException> { parse("WHILE -1 : PRINT 1 : WEND") }
    }
}
