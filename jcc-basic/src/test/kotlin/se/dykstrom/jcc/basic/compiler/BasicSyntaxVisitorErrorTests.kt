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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.assertLines
import se.dykstrom.jcc.basic.BasicTests.Companion.assertMessageContains

/**
 * Tests the mistakes `Basic.g4` accepts only so that [BasicSyntaxVisitor] can name them. The
 * grammar has to parse them: rejecting a mistake on a block header line costs the whole block,
 * because the parser gives up on the rule and orphans every terminator inside it.
 *
 * @author Johan Dykstrom
 */
class BasicSyntaxVisitorErrorTests : AbstractBasicSyntaxVisitorTests() {

    // ELSE IF written as two words:

    @Test
    fun shouldReportElseIfWrittenAsTwoWords() {
        val errors = parseCollectingErrors(
            """
                IF a THEN
                    PRINT 1
                ELSE IF b THEN
                    PRINT 2
                ELSEIF c THEN
                    PRINT 3
                ELSE
                    PRINT 4
                END IF
            """.trimIndent()
        )
        assertLines(errors, 3)
        assertMessageContains(errors, "'ELSE IF' is not 'ELSEIF'")
    }

    @Test
    fun shouldPointElseIfErrorAtTheElse() {
        // The ELSE is what has to change, so that is where the caret belongs
        val errors = parseCollectingErrors("IF a THEN\n    PRINT 1\n    ELSE IF b THEN\n    PRINT 2\nEND IF\n")
        assertLines(errors, 3)
        assertEquals(4, errors[0].column())
    }

    @Test
    fun shouldPointElseIfErrorAtTheElseAfterALineNumber() {
        val errors = parseCollectingErrors("10 IF a THEN\n20 PRINT 1\n30 ELSE IF b THEN\n40 PRINT 2\n50 END IF\n")
        assertLines(errors, 3)
        assertEquals(3, errors[0].column())
    }

    @Test
    fun shouldReportEveryElseIfWrittenAsTwoWords() {
        // visitIfThenBlock walks the ELSEIF blocks in reverse, so these reach the listener
        // bottom-up. CompilationMessage.compareTo is what puts them back in reading order, which
        // is the order they are printed in
        val errors = parseCollectingErrors("IF a THEN\nPRINT 1\nELSE IF b THEN\nPRINT 2\nELSE IF c THEN\nPRINT 3\nEND IF\n")
        assertLines(errors.sorted(), 3, 5)
    }

    @Test
    fun shouldNotReportElseIfWrittenAsOneWord() {
        // parse itself asserts that the visitor reported nothing
        parse("IF a THEN\nPRINT 1\nELSEIF b THEN\nPRINT 2\nEND IF\n")
    }

    @Test
    fun shouldNotReportSingleLineIfWithElseIf() {
        // Valid QuickBASIC: an ELSE holding a single-line IF, not the two-word ELSEIF mistake
        parse("IF a THEN PRINT 1 ELSE IF b THEN PRINT 2\n")
    }

    @Test
    fun shouldNotReportNestedIfOnItsOwnLineInsideElse() {
        parse("IF a THEN\nPRINT 1\nELSE\nIF b THEN\nPRINT 2\nEND IF\nEND IF\n")
    }
}
