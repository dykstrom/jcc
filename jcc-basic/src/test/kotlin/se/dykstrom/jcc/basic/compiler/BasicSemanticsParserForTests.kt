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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_0_5
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_1_2
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_2_0
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_1
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_3
import se.dykstrom.jcc.basic.BasicTests.Companion.castToFloat
import se.dykstrom.jcc.basic.BasicTests.Companion.castToInt
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.error.Warning.FLOAT_CONVERSION
import se.dykstrom.jcc.common.error.Warning.FOR_COUNTER_CHANGE
import se.dykstrom.jcc.common.error.Warning.UNDEFINED_VARIABLE
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64

/**
 * Tests class `BasicSemanticsParser`, especially functionality related to FOR statements.
 *
 * @see BasicSemanticsParser
 */
class BasicSemanticsParserForTests : AbstractBasicSemanticsParserTests() {

    @Test
    fun shouldTypeCounterBySuffix() {
        assertEquals(I64.INSTANCE, forOf("FOR i% = 1 TO 3: NEXT").counter.identifier.type())
        assertEquals(F64.INSTANCE, forOf("FOR i# = 1 TO 3: NEXT").counter.identifier.type())
    }

    @Test
    fun shouldTypeCounterByDefType() {
        assertEquals(I64.INSTANCE, forOf("DEFINT i: FOR i = 1 TO 3: NEXT").counter.identifier.type())
    }

    @Test
    fun shouldTypeCounterByDim() {
        assertEquals(I64.INSTANCE, forOf("DIM k AS INTEGER: FOR k = 1 TO 3: NEXT").counter.identifier.type())
    }

    @Test
    fun shouldTypeCounterByDefaultType() {
        assertEquals(F64.INSTANCE, forOf("FOR x = 1 TO 3: NEXT").counter.identifier.type())
    }

    @Test
    fun shouldCastIntegerExpressionsToDoubleCounter() {
        val fs = forOf("FOR x = 1 TO 3 STEP 1: NEXT")
        assertEquals(castToFloat(IL_1), fs.start)
        assertEquals(castToFloat(IL_3), fs.end)
        assertEquals(castToFloat(IL_1), fs.step)
    }

    @Test
    fun shouldRoundFloatExpressionsToIntegerCounter() {
        val fs = forOf("FOR i% = 0.5 TO 2.0 STEP 1.2: NEXT")
        assertEquals(castToInt(FL_0_5), fs.start)
        assertEquals(castToInt(FL_2_0), fs.end)
        assertEquals(castToInt(FL_1_2), fs.step)
    }

    @Test
    fun shouldWarnAboutFloatToIntegerConversion() {
        parseAndExpectWarning("FOR i% = 1 TO 2.5: NEXT", "implicit conversion turns floating-point number into integer", FLOAT_CONVERSION)
    }

    @Test
    fun shouldDefineCounterForLoopBody() {
        parse("FOR i% = 1 TO 3: PRINT i%: NEXT")
        // The counter is undefined where the loop assigns it, as in an assignment, but not in the body
        assertEquals(1, errorListener.warnings.count { it.warning == UNDEFINED_VARIABLE })
    }

    @Test
    fun shouldCheckLoopBody() {
        parseAndExpectException("FOR i% = 1 TO 3: PRINT 1 + \"a\": NEXT", "cannot add")
    }

    @Test
    fun shouldRejectStringStart() {
        parseAndExpectOneException("FOR i% = \"a\" TO 3: NEXT", "expression of type string not allowed in FOR statement")
    }

    @Test
    fun shouldRejectStringStep() {
        parseAndExpectOneException("FOR i% = 1 TO 3 STEP \"a\": NEXT", "expression of type string not allowed in FOR statement")
    }

    @Test
    fun shouldRejectStringCounter() {
        parseAndExpectOneException("FOR s$ = 1 TO 3: NEXT", "FOR index variable 's$' must be numeric, not string")
    }

    @Test
    fun shouldRejectStringCounterWithStringExpressionsOnlyOnce() {
        parseAndExpectOneException("FOR s$ = \"a\" TO \"z\": NEXT", "FOR index variable 's$' must be numeric, not string")
    }

    @Test
    fun shouldCheckExpressionsOfStringCounter() {
        parseAndExpectException("FOR s$ = 1 + \"a\" TO 3: NEXT", "cannot add")
    }

    @Test
    fun shouldRejectConstantCounter() {
        parseAndExpectOneException("CONST c% = 1: FOR c% = 1 TO 3: NEXT", "FOR index variable 'c%' is a constant")
    }

    @Test
    fun shouldRejectCounterInUse() {
        parseAndExpectOneException("FOR i = 1 TO 2\nFOR i = 1 TO 2\nNEXT\nNEXT\n", "FOR index variable 'i' already in use")
        assertEquals(listOf(2), errorListener.errors.map { it.line })
    }

    @Test
    fun shouldRejectCounterInUseInsideIf() {
        parseAndExpectOneException("FOR i = 1 TO 2\nIF i = 1 THEN\nFOR i = 1 TO 2: NEXT\nEND IF\nNEXT\n", "FOR index variable 'i' already in use")
    }

    @Test
    fun shouldRejectCounterInUseInsideWhile() {
        parseAndExpectOneException("FOR i = 1 TO 2\nWHILE 0\nFOR i = 1 TO 2: NEXT\nWEND\nNEXT\n", "FOR index variable 'i' already in use")
    }

    @Test
    fun shouldRejectCounterInUseByOuterLoop() {
        parseAndExpectOneException("FOR i = 1 TO 2: FOR j = 1 TO 2: FOR i = 1 TO 2: NEXT i, j, i", "FOR index variable 'i' already in use")
    }

    @Test
    fun shouldAcceptCounterReusedBySequentialLoops() {
        parse("FOR i = 1 TO 2: NEXT\nFOR i = 1 TO 2: NEXT\n")
        assertFalse(errorListener.hasErrors())
    }

    @Test
    fun shouldAcceptCounterReusedAfterNestedLoop() {
        parse("FOR i = 1 TO 2: FOR j = 1 TO 2: NEXT j: FOR j = 1 TO 2: NEXT j: NEXT i")
        assertFalse(errorListener.hasErrors())
    }

    @Test
    fun shouldRejectExitForOutsideFor() {
        parseAndExpectOneException("EXIT FOR", "EXIT FOR not within FOR...NEXT")
    }

    @Test
    fun shouldRejectExitForInsideWhileOutsideFor() {
        parseAndExpectOneException("PRINT 1\nWHILE 0\n  EXIT FOR\nWEND\n", "EXIT FOR not within FOR...NEXT")
        assertEquals(listOf(3), errorListener.errors.map { it.line })
        assertEquals(listOf(2), errorListener.errors.map { it.column })
    }

    @Test
    fun shouldRejectExitForInsideIfOutsideFor() {
        parseAndExpectOneException("IF 1 THEN EXIT FOR", "EXIT FOR not within FOR...NEXT")
    }

    @Test
    fun shouldRejectExitForAfterLoop() {
        parseAndExpectOneException("FOR i = 1 TO 2: NEXT\nEXIT FOR\n", "EXIT FOR not within FOR...NEXT")
        assertEquals(listOf(2), errorListener.errors.map { it.line })
    }

    @Test
    fun shouldAcceptExitForInsideWhileAndIfInsideFor() {
        parse("FOR i% = 1 TO 2\nWHILE 0\nIF i% THEN EXIT FOR\nWEND\nNEXT\n")
        assertFalse(errorListener.hasErrors())
    }

    @Test
    fun shouldWarnAboutAssignmentToCounter() {
        parseAndExpectWarning("FOR i% = 1 TO 3\n    i% = 10\nNEXT\n", "FOR index variable 'i%' changed inside its loop", FOR_COUNTER_CHANGE)
        val warning = counterWarnings().single()
        assertEquals(2, warning.line)
        assertEquals(4, warning.column)
    }

    @Test
    fun shouldWarnAboutSwapWithCounter() {
        parse("FOR i% = 1 TO 3: SWAP i%, j%: NEXT")
        assertEquals(listOf("FOR index variable 'i%' changed inside its loop"), counterWarnings().map { it.msg })
    }

    @Test
    fun shouldWarnAboutSwapOfTwoCounters() {
        parse("FOR i% = 1 TO 3: FOR j% = 1 TO 3: SWAP i%, j%: NEXT j%, i%")
        assertEquals(2, counterWarnings().size)
    }

    @Test
    fun shouldWarnAboutAssignmentToOuterCounter() {
        parseAndExpectWarning("FOR i% = 1 TO 3: FOR j% = 1 TO 3: i% = 10: NEXT j%, i%", "FOR index variable 'i%' changed inside its loop", FOR_COUNTER_CHANGE)
    }

    @Test
    fun shouldWarnAboutAssignmentToCounterInsideIf() {
        parseAndExpectWarning("FOR i% = 1 TO 3\nIF i% = 2 THEN i% = 10\nNEXT\n", "FOR index variable 'i%' changed inside its loop", FOR_COUNTER_CHANGE)
    }

    @Test
    fun shouldWarnAboutAssignmentToCounterInsideWhile() {
        parseAndExpectWarning("FOR i% = 1 TO 3\nWHILE 0\ni% = 10\nWEND\nNEXT\n", "FOR index variable 'i%' changed inside its loop", FOR_COUNTER_CHANGE)
    }

    @Test
    fun shouldNotWarnAboutAssignmentToCounterAfterLoop() {
        parse("FOR i% = 1 TO 3: NEXT\ni% = 10\n")
        assertEquals(0, counterWarnings().size)
    }

    @Test
    fun shouldNotWarnAboutAssignmentToOtherVariable() {
        parse("FOR i% = 1 TO 3: j% = 10: NEXT")
        assertEquals(0, counterWarnings().size)
    }

    @Test
    fun shouldNotWarnAboutAssignmentToArrayElementWithCounterName() {
        parse("DIM i%(3) AS INTEGER\nFOR i% = 1 TO 3: i%(1) = 10: NEXT\n")
        assertFalse(errorListener.hasErrors())
        assertEquals(0, counterWarnings().size)
    }

    @Test
    fun shouldNotWarnAboutAssignmentToVariableWithOtherSuffix() {
        parse("FOR i% = 1 TO 3: i = 10: NEXT")
        assertEquals(0, counterWarnings().size)
    }

    private fun counterWarnings() = errorListener.warnings.filter { it.warning == FOR_COUNTER_CHANGE }

    private fun forOf(text: String): ForStatement = parse(text).statements.filterIsInstance<ForStatement>().single()
}
