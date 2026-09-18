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

/**
 * Tests that one type error produces one message. An expression whose type could not be
 * determined gets the unknown type, and every check accepts it, so the enclosing construct
 * does not report a second mistake about the type the compiler fell back to. Issue #86, item 9.
 *
 * @author Johan Dykstrom
 */
class BasicSemanticsParserCascadeTests : AbstractBasicSemanticsParserTests() {

    @Test
    fun shouldNotReportAssignmentAfterIllegalExpression() {
        // The assignment used to be reported first, and as a double, which is the type the
        // failed type computation fell back to
        parseAndExpectOneException("DIM b AS STRING\nb = 1 - \"x\"\n", "illegal expression: 1 - \"x\"")
    }

    @Test
    fun shouldNotReportIfConditionAfterIllegalExpression() {
        parseAndExpectOneException("DIM s AS STRING\nIF s - 1 THEN\nPRINT 1\nEND IF\n", "illegal expression")
    }

    @Test
    fun shouldNotReportWhileConditionAfterIllegalExpression() {
        parseAndExpectOneException("DIM s AS STRING\nWHILE s - 1\nWEND\n", "illegal expression")
    }

    @Test
    fun shouldNotReportOnGotoExpressionAfterIllegalExpression() {
        parseAndExpectOneException("DIM s AS STRING\nON s - 1 GOTO 10\n10 PRINT 1\n", "illegal expression")
    }

    @Test
    fun shouldNotReportBitwiseOperatorAfterIllegalExpression() {
        parseAndExpectOneException("DIM s AS STRING\nPRINT (s - 1) AND 1\n", "illegal expression")
    }

    @Test
    fun shouldNotReportNotAfterIllegalExpression() {
        parseAndExpectOneException("DIM s AS STRING\nPRINT NOT (s - 1)\n", "illegal expression")
    }

    @Test
    fun shouldNotReportNegationAfterIllegalExpression() {
        parseAndExpectOneException("DIM s AS STRING\nPRINT -(s - 1)\n", "illegal expression")
    }

    @Test
    fun shouldNotReportComparisonAfterIllegalExpression() {
        parseAndExpectOneException("DIM s AS STRING\nPRINT (s - 1) > 1\n", "illegal expression")
    }

    @Test
    fun shouldNotReportFunctionCallAfterIllegalExpression() {
        // No overload can match an argument that has already been reported, and the candidate
        // list would bury the real mistake
        parseAndExpectOneException("PRINT sin(\"x\" - 1)\n", "illegal expression")
    }

    @Test
    fun shouldNotReportRandomizeAfterIllegalExpression() {
        parseAndExpectOneException("DIM s AS STRING\nRANDOMIZE s - 1\n", "illegal expression")
    }

    @Test
    fun shouldNotReportSleepAfterIllegalExpression() {
        parseAndExpectOneException("DIM s AS STRING\nSLEEP s - 1\n", "illegal expression")
    }

    @Test
    fun shouldStillReportTheSecondMistakeOfTwo() {
        // Suppressing the cascade must not suppress an independent mistake
        val text = "DIM b AS STRING\nb = 1 - \"x\"\nPRINT NOT 1.5\n"
        parseAndExpectException(text, "illegal expression: 1 - \"x\"")
        parseAndExpectException(text, "expected subexpression of type integer")
    }
}
