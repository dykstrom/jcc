/*
 * Copyright (C) 2026 Johan Dykstrom
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

class BasicParserForTests : AbstractBasicParserTests() {

    @Test
    fun shouldParseFor() {
        parse("""
            FOR i = 1 TO 10
              PRINT i
            NEXT i
        """)
        parse("""
            10 FOR i% = a% TO b% + 1 STEP -1
            20 NEXT
        """)
    }

    @Test
    fun shouldParseOneLineFor() {
        parse("FOR i = 1 TO 3: PRINT i: NEXT")
        parse("FOR i = 1 TO 3: PRINT i: NEXT i")
    }

    @Test
    fun shouldParseNextWithManyCounters() {
        parse("""
            FOR i = 1 TO 3
              FOR j = 1 TO 3
                PRINT i * j
            NEXT j, i
        """)
    }

    @Test
    fun shouldParseForInSingleLineIf() {
        parse("IF a THEN FOR i = 1 TO 3: PRINT i: NEXT ELSE PRINT 0")
    }

    @Test
    fun shouldParseExitFor() {
        parse("""
            FOR i = 1 TO 10
              IF i = 5 THEN EXIT FOR
              IF i = 6 THEN
                EXIT FOR
              END IF
              WHILE i > 7
                EXIT FOR
              WEND
            NEXT
        """)
        parse("FOR i = 1 TO 3: PRINT i: EXIT FOR: NEXT")
    }

    @Test
    fun shouldParseMalformedExit() {
        // The grammar accepts them so that BasicSyntaxVisitor can name the mistake
        parse("EXIT")
        parse("EXIT DEF")
        parse("EXIT 1 + 2: PRINT 3")
    }

    @Test
    fun shouldNotParseExitForWithTrailingTokens() {
        assertThrows<IllegalStateException> { parse("FOR i = 1 TO 3\nEXIT FOR i\nNEXT") }
    }

    @Test
    fun shouldNotParseForWithoutTo() {
        assertThrows<IllegalStateException> { parse("FOR i = 1\nNEXT") }
    }

    @Test
    fun shouldParseForWithArrayElementCounter() {
        // The grammar accepts it so that BasicSyntaxVisitor can name the mistake
        parse("FOR a(1) = 1 TO 3\nNEXT")
    }
}
