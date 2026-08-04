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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.error.CompilationError

/**
 * Tests how the parser recovers from a syntax error: one mistake must produce one message, and the
 * lines after it must be parsed as if the mistake had not happened. A statement ends at the end of
 * its line, so the next line is where recovery resumes.
 *
 * @author Johan Dykstrom
 */
class BasicParserRecoveryTests : AbstractBasicParserTests() {

    // One mistake, one message:

    @Test
    fun shouldReportBrokenLineInsideBlockOnlyOnce() {
        // The lines after the broken one are all correct, and none of them may be reported
        val errors = parseCollectingErrors(
            """
                DIM c AS STRING
                WHILE c <> "q"
                    IF c = "a" THEN
                        PRINT "one"
                    ELSEIF c = "b" THEN
                        PRINT "two " ;
                              "three"
                    ELSEIF c = "c" THEN
                        PRINT "four"
                    ELSE
                        PRINT "five"
                    END IF
                WEND
            """
        )
        assertNoMessageContains(errors, "without matching")
        assertLines(errors, 7)
    }

    @Test
    fun shouldReportOnlyTheFirstErrorOnALine() {
        // Everything after the first error on a line is a guess about text already lost
        assertLines(parseCollectingErrors("PRINT 1 ; ; ; 2\n"), 1)
    }

    @Test
    fun shouldNotReportLinesFollowingABrokenLineAtTopLevel() {
        val errors = parseCollectingErrors(
            """
                DIM a AS INTEGER
                a = 1
                * 2
                PRINT a
                PRINT a + 1
            """
        )
        assertLines(errors, 4)
    }

    // Independent mistakes are all reported:

    @Test
    fun shouldReportEveryIndependentError() {
        val errors = parseCollectingErrors(
            """
                DIM a AS INTEGER
                a = 1 +
                PRINT a
                a = * 2
                PRINT a
            """
        )
        assertLines(errors, 3, 5)
    }

    @Test
    fun shouldReportErrorsInSeparateBlocks() {
        val errors = parseCollectingErrors(
            """
                WHILE a
                    PRINT 1 +
                WEND
                WHILE b
                    PRINT 2 +
                WEND
            """
        )
        assertLines(errors, 3, 6)
    }

    // A block whose terminator is present is never reported as unterminated:

    @Test
    fun shouldNotClaimBlockIsUnterminatedAfterErrorInItsBody() {
        // Both the WHILE and the IF are terminated. Recovery from the error on line 5 used to
        // leave the parser in the WHILE's context, which was then reported as a missing WEND —
        // naming a loop the reader can see is closed on line 8.
        val errors = parseCollectingErrors(
            """
                WHILE a
                    IF b THEN
                        PRINT 1
                    ELSEIF c THEN
                        PRINT 2 +
                    ELSE
                        PRINT 3
                    END IF
                WEND
            """
        )
        assertNoMessageContains(errors, "without matching")
        assertLines(errors, 6)
    }

    @Test
    fun shouldStillReportUnterminatedBlockAsTheOnlyError() {
        val errors = parseCollectingErrors(
            """
                WHILE a
                    PRINT 1
            """
        )
        assertEquals(1, errors.size, "expected one error, got: ${errors.map { it.msg() }}")
        assertMessageContains(errors, "WHILE without matching WEND, WHILE at line 2")
    }

    @Test
    fun shouldStillReportUnterminatedBlockAfterErrorOnItsOpeningLine() {
        // The error is in the WHILE header, not in its body, so the loop still needs a WEND
        val errors = parseCollectingErrors(
            """
                WHILE a +
                    PRINT 1
            """
        )
        assertMessageContains(errors, "WHILE without matching WEND, WHILE at line 2")
    }

    @Test
    fun shouldStillReportUnterminatedBlockAfterErrorBeforeIt() {
        // The earlier error is outside the loop and says nothing about whether it is terminated
        val errors = parseCollectingErrors(
            """
                PRINT 1 +
                WHILE a
                    PRINT 2
            """
        )
        assertMessageContains(errors, "WHILE without matching WEND, WHILE at line 3")
    }

    // A statement wrongly continued onto the next line:

    @Test
    fun shouldReportStatementContinuedAfterSemicolon() {
        val errors = parseCollectingErrors("PRINT \"a\" ;\n      \"b\"\n")
        assertMessageContains(errors, "';' at the end of a line")
        assertMessageContains(errors, "end the line with '_' to continue it")
    }

    @Test
    fun shouldReportStatementContinuedAfterComma() {
        val errors = parseCollectingErrors("PRINT \"a\" ,\n      \"b\"\n")
        assertMessageContains(errors, "',' at the end of a line")
    }

    @Test
    fun shouldPointContinuationErrorAtTheSeparator() {
        // The separator is the character to remove, so that is where the caret belongs, even
        // though the parse failed on the line below it
        val errors = parseCollectingErrors("PRINT \"a\" ;\n      \"b\"\n")
        assertEquals(1, errors.size, "expected one error, got: ${errors.map { it.msg() }}")
        assertEquals(1, errors[0].line())
        assertEquals(10, errors[0].column())
    }

    @Test
    fun shouldReportContinuationWhenNextLineStartsWithIdentifier() {
        val errors = parseCollectingErrors("DIM a AS INTEGER\nPRINT \"x\" ;\n      a ; \"y\"\n")
        assertMessageContains(errors, "';' at the end of a line")
    }

    @Test
    fun shouldNotReportContinuationWhenNextLineStartsAStatement() {
        // A line beginning with a statement keyword is a statement of its own, however badly the
        // line in front of it ended
        val errors = parseCollectingErrors("PRINT \"a\" ;\nPRINT 1 +\n")
        assertNoMessageContains(errors, "at the end of a line")
        assertLines(errors, 2)
    }

    @Test
    fun shouldNotReportContinuationWhenPreviousLineDoesNotEndWithSeparator() {
        val errors = parseCollectingErrors("PRINT \"a\"\n      * 2\n")
        assertNoMessageContains(errors, "at the end of a line")
    }

    @Test
    fun shouldNotReportContinuationForErrorOnFirstLine() {
        // There is no line in front of the first one for a statement to have continued from
        val errors = parseCollectingErrors("* 2\n")
        assertNoMessageContains(errors, "at the end of a line")
    }

    // An expression continued onto the next line without a trailing '_':

    @Test
    fun shouldReportUnclosedParenthesisAtEndOfLine() {
        val errors = parseCollectingErrors("PRINT sqr(1 + 2\n          + 3)\n")
        assertEquals(1, errors.size, "expected one error, got: ${errors.map { it.msg() }}")
        assertMessageContains(errors, "'(' is not closed before the end of the line")
        assertMessageContains(errors, "end the line with '_' to continue the statement onto the next line")
    }

    @Test
    fun shouldPointUnclosedParenthesisErrorAtTheParenthesis() {
        // The parenthesis is what the reader has to look at, even though the parse failed on the
        // line break after it
        val errors = parseCollectingErrors("PRINT sqr(1 + 2\n          + 3)\n")
        assertEquals(1, errors[0].line())
        assertEquals(9, errors[0].column())
    }

    @Test
    fun shouldReportTrailingOperatorRatherThanUnclosedParenthesis() {
        // Both are true of this line, and the operator is the more precise of the two
        val errors = parseCollectingErrors("PRINT sqr(1 + 2 +\n          3)\n")
        assertEquals(1, errors.size, "expected one error, got: ${errors.map { it.msg() }}")
        assertMessageContains(errors, "expression expected after '+'")
        assertMessageContains(errors, "end the line with '_' to continue the statement onto the next line")
    }

    @Test
    fun shouldReportUnclosedParenthesisInFunctionDefinition() {
        val errors = parseCollectingErrors("DEF FNhyp(a, b) = sqr(a * a\n                      + b * b)\n")
        assertEquals(1, errors.size, "expected one error, got: ${errors.map { it.msg() }}")
        assertMessageContains(errors, "'(' is not closed before the end of the line")
    }

    @Test
    fun shouldNotSuggestContinuationWhenNoLineFollows() {
        val errors = parseCollectingErrors("PRINT 1 +\n")
        assertMessageContains(errors, "expression expected after '+'")
        assertNoMessageContains(errors, "'_'")
    }

    @Test
    fun shouldNotSuggestContinuationWhenNextLineStartsAStatement() {
        // A line beginning with a statement keyword is a statement of its own, so the line in front
        // of it is simply incomplete
        val errors = parseCollectingErrors("PRINT sqr(1\nPRINT 2\n")
        assertMessageContains(errors, "'(' is not closed before the end of the line")
        assertNoMessageContains(errors, "'_'")
        assertLines(errors, 1)
    }

    @Test
    fun shouldStillReportErrorsAfterAnExpressionRanOffItsLine() {
        val errors = parseCollectingErrors("PRINT sqr(1 + 2\n          + 3)\nPRINT 1 ; ; 2\n")
        assertLines(errors, 1, 3)
    }

    @Test
    fun shouldNotReportRunOffLineForACompleteLine() {
        // Nothing is missing at the end of the line, so the mistake is elsewhere on it
        val errors = parseCollectingErrors("PRINT 1 2\n")
        assertNoMessageContains(errors, "end of the line")
        assertNoMessageContains(errors, "expression expected")
    }

    // A trailing separator that is doing its real job must keep parsing:

    @Test
    fun shouldParseTrailingSemicolonSuppressingTheLineBreak() {
        parse("PRINT \"a\" ;\nPRINT \"b\"\n")
        parse("PRINT \"a\" ,\nPRINT \"b\"\n")
    }

    @Test
    fun shouldParseTrailingSemicolonFollowedByLineNumber() {
        parse("PRINT \"a\" ;\n20 PRINT \"b\"\n")
    }

    private fun assertLines(errors: List<CompilationError>, vararg lines: Int) {
        assertEquals(
            lines.toList(), errors.map { it.line() },
            "\nExpected errors on lines ${lines.toList()}\nActual: ${errors.map { "${it.line()}:${it.column()} ${it.msg()}" }}"
        )
    }

    private fun assertMessageContains(errors: List<CompilationError>, expected: String) {
        assertTrue(
            errors.any { it.msg().contains(expected) },
            "\nExpected some message to contain: '$expected'\nActual: ${errors.map { it.msg() }}"
        )
    }

    private fun assertNoMessageContains(errors: List<CompilationError>, unexpected: String) {
        assertTrue(
            errors.none { it.msg().contains(unexpected) },
            "\nExpected no message to contain: '$unexpected'\nActual: ${errors.map { it.msg() }}"
        )
    }
}
