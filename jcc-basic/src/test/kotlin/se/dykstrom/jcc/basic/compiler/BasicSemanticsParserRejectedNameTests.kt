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
import se.dykstrom.jcc.common.error.Warning.UNDEFINED_VARIABLE
import se.dykstrom.jcc.common.error.Warning.UNUSED_VARIABLE

/**
 * Tests that a name the syntax visitor rejects as a variable name is not also warned about
 * as an undefined or unused variable (issue #116).
 */
class BasicSemanticsParserRejectedNameTests : AbstractBasicSemanticsParserTests() {

    @Test
    fun shouldNotWarnAboutAssignedReservedWord() {
        parseAndExpectOnlyError("print = 5\n", "'print' is a reserved word and cannot be used as a variable name")
    }

    @Test
    fun shouldNotWarnAboutDeclaredReservedWord() {
        parseAndExpectOnlyError("dim goto as integer\n", "'goto' is a reserved word and cannot be used as a variable name")
    }

    @Test
    fun shouldNotWarnAboutAssignedUnderscoreName() {
        parseAndExpectOnlyError("x_y = 3\n", "'x_y' is not a valid name")
    }

    @Test
    fun shouldNotWarnAboutDeclaredUnderscoreName() {
        parseAndExpectOnlyError("dim c_d as integer\n", "'c_d' is not a valid name")
    }

    @Test
    fun shouldNotWarnAboutUnderscoreArray() {
        parseAndExpectOnlyError("print a_b(1)\n", "'a_b' is not a valid name")
    }

    @Test
    fun shouldNotWarnAboutSingleSuffix() {
        parseAndExpectOnlyError("a! = 3\n", "type suffix '!' (single precision) is not supported by JCC")
    }

    @Test
    fun shouldNotWarnAboutLongSuffix() {
        parseAndExpectOnlyError("b& = 3\n", "type suffix '&' (long) is not supported by JCC")
    }

    @Test
    fun shouldNotWarnAboutForArrayElement() {
        parseAndExpectOnlyError("for a(1) = 1 to 2: next\n", "FOR index variable must be a simple variable")
    }

    @Test
    fun shouldNotWarnAboutAssignedFnName() {
        parseAndExpectOnlyError("fnx = 5\n", "'fnx' starts with FN")
    }

    @Test
    fun shouldNotWarnAboutDeclaredFnName() {
        parseAndExpectOnlyError("dim fny\n", "'fny' starts with FN")
    }

    @Test
    fun shouldNotWarnAboutFnNameConstant() {
        parseAndExpectOnlyError("const fnx = 5\n", "'fnx' starts with FN")
    }

    @Test
    fun shouldNotWarnAboutFnNameInSwap() {
        parseAndExpectOnlyError("dim y\ny = 1\nswap fnx, y\n", "'fnx' starts with FN")
    }

    @Test
    fun shouldNotWarnAboutFnNameInLineInput() {
        parseAndExpectOnlyError("line input fnx$\n", "'fnx$' starts with FN")
    }

    @Test
    fun shouldNotWarnAboutFnNameParameter() {
        parseAndExpectOnlyError("def fna(fnx) = 1\n", "'fnx' starts with FN")
    }

    @Test
    fun shouldStillWarnAboutOtherNames() {
        parseAndExpectException("print = 5\nprint z\n", "'print' is a reserved word")
        assertTrue(errorListener.warnings.any { it.warning == UNDEFINED_VARIABLE && it.msg == "undefined variable: z" })
    }

    @Test
    fun shouldStillWarnAboutUnusedParameterWithMissingFnPrefix() {
        parseAndExpectException("def foo(x) = 1\n", "FN")
        assertTrue(errorListener.warnings.any { it.warning == UNUSED_VARIABLE && it.msg == "unused variable: x" })
    }

    private fun parseAndExpectOnlyError(text: String, expectedMessage: String) {
        parseAndExpectOneException(text, expectedMessage)
        assertTrue(errorListener.warnings.isEmpty(), "Expected no warnings, found: ${errorListener.warnings}")
    }
}
