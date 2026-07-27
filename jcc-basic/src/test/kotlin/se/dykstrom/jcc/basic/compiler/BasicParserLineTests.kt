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

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Tests the QuickBASIC 4.5 line rule: a statement ends at the end of its line, unless the line
 * ends with an underscore. COLON separates statements within a line.
 *
 * @author Johan Dykstrom
 */
class BasicParserLineTests : AbstractBasicParserTests() {

    // End of line terminates a statement:

    @Test
    fun shouldNotContinueStatementOnNextLine() {
        // A line beginning with an operator is an error, not a continuation of the line above
        val exception = assertThrows<IllegalStateException> {
            parse("""
                DIM a AS INTEGER
                a = 1
                -2
                PRINT a
            """)
        }
        assertErrorOnLine(exception, 4)
    }

    @Test
    fun shouldReportIncompleteExpressionOnItsOwnLine() {
        // The dangling + is on line 3, and that is where the error belongs
        val exception = assertThrows<IllegalStateException> {
            parse("""
                DIM a AS INTEGER
                a = 1 +
                PRINT a
            """)
        }
        assertErrorOnLine(exception, 3)
    }

    @Test
    fun shouldReportUnclosedParenthesisOnItsOwnLine() {
        val exception = assertThrows<IllegalStateException> { parse("PRINT (1 + 2") }
        assertErrorOnLine(exception, 1)
    }

    @Test
    fun shouldNotParseTwoStatementsOnOneLineWithoutColon() {
        assertThrows<IllegalStateException> { parse("PRINT 1 PRINT 2") }
    }

    @Test
    fun shouldNotParseTwoNumberedStatementsOnOneLine() {
        assertThrows<IllegalStateException> { parse("10 PRINT 1 20 PRINT 2") }
    }

    // Unterminated blocks:

    @Test
    fun shouldReportIfWithoutEndIf() {
        val exception = assertThrows<IllegalStateException> {
            parse("""
                a% = 1
                IF a% = 1 THEN
                PRINT "one"
                PRINT "two"
                PRINT "done"
            """)
        }
        assertMessageContains(exception, "IF without matching END IF, IF at line 3")
    }

    @Test
    fun shouldReportWhileWithoutWend() {
        val exception = assertThrows<IllegalStateException> {
            parse("""
                WHILE a
                PRINT 1
            """)
        }
        assertMessageContains(exception, "WHILE without matching WEND, WHILE at line 2")
    }

    @Test
    fun shouldReportInnermostUnterminatedBlock() {
        // The WHILE is terminated, the IF inside it is not
        val exception = assertThrows<IllegalStateException> {
            parse("""
                WHILE a
                  IF b THEN
                    PRINT 1
                WEND
            """)
        }
        assertMessageContains(exception, "IF without matching END IF, IF at line 3")
    }

    @Test
    fun shouldReportElseBlockWithoutEndIf() {
        val exception = assertThrows<IllegalStateException> {
            parse("""
                IF a THEN
                PRINT 1
                ELSE
                PRINT 2
            """)
        }
        assertMessageContains(exception, "IF without matching END IF, IF at line 2")
    }

    @Test
    fun shouldNotReportUnterminatedBlockForErrorInsideBody() {
        // The block is terminated; the error is in the body and must be reported as such
        val exception = assertThrows<IllegalStateException> {
            parse("""
                IF a THEN
                  PRINT 1 +
                END IF
            """)
        }
        assertMessageDoesNotContain(exception, "without matching")
    }

    // Line continuation:

    @Test
    fun shouldContinueLineEndingWithUnderscore() {
        parse("""
            DIM a AS INTEGER
            a = 1 + _
                2
            PRINT a
        """)
    }

    @Test
    fun shouldContinueSeveralTimes() {
        parse("PRINT 1 + _\n2 + _\n3\n")
    }

    @Test
    fun shouldContinueLineWithTrailingWhitespaceAfterUnderscore() {
        parse("PRINT 1 + _  \t \n2\n")
    }

    @Test
    fun shouldNotContinueCommentLine() {
        // The underscore belongs to the comment, so PRINT 1 is a statement of its own
        parse("""
            ' a comment ending in _
            PRINT 1
        """)
    }

    @Test
    fun shouldNotContinueRemLine() {
        parse("""
            REM a remark ending in _
            PRINT 1
        """)
    }

    @Test
    fun shouldNotTreatUnderscoreInStringAsContinuation() {
        parse("""
            PRINT "an underscore _ inside a string"
            PRINT 1
        """)
    }

    // Lines that must still parse:

    @Test
    fun shouldParseColonSeparatedStatements() {
        parse("a = 1 : b = 2 : PRINT a; b")
        parse("10 a = 1 : PRINT a")
    }

    @Test
    fun shouldParseBlankLines() {
        parse("\n\n\nPRINT 1\n\n\nPRINT 2\n\n\n")
    }

    @Test
    fun shouldParseBlankLinesContainingWhitespace() {
        // A line holding nothing but spaces or tabs is still a blank line
        parse("\n   \n\t\nPRINT 1\n  \n\t \nPRINT 2\n   \n")
    }

    @Test
    fun shouldParseCommentOnlyLines() {
        parse("""
            ' first
            REM second
            PRINT 1
            ' third
        """)
    }

    @Test
    fun shouldParseLabelAloneOnItsLine() {
        parse("""
            GOSUB printIt
            END

            printIt:
            PRINT 1
            RETURN
        """)
    }

    @Test
    fun shouldParseLineNumberAloneOnItsLine() {
        parse("""
            10
            20 PRINT 1
        """)
    }

    @Test
    fun shouldParseFileWithoutTrailingNewline() {
        parse("PRINT 1")
        parse("IF a THEN\nPRINT 1\nEND IF")
    }

    @Test
    fun shouldParseTabIndentedProgram() {
        parse("IF a THEN\n\tPRINT 1\n\tPRINT 2\nEND IF\n")
    }

    @Test
    fun shouldParseCommentAfterBlockOpener() {
        // In QuickBASIC a comment after THEN does not make the IF a single-line IF
        parse("""
            IF a THEN ' check a
              PRINT 1
            ELSEIF b THEN ' check b
              PRINT 2
            ELSE ' neither
              PRINT 3
            END IF
        """)
    }

    private fun assertErrorOnLine(exception: IllegalStateException, line: Int) {
        assertMessageContains(exception, "Syntax error at $line:")
    }

    private fun assertMessageContains(exception: IllegalStateException, expected: String) {
        val message = exception.message ?: ""
        assertTrue(message.contains(expected), "\nExpected to contain: '$expected'\nActual: '$message'")
    }

    private fun assertMessageDoesNotContain(exception: IllegalStateException, unexpected: String) {
        val message = exception.message ?: ""
        assertTrue(!message.contains(unexpected), "\nExpected NOT to contain: '$unexpected'\nActual: '$message'")
    }
}
