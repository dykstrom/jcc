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
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_1
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_2
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_3
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_5
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_M1
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_I64_B
import se.dykstrom.jcc.basic.BasicTests.Companion.assertLines
import se.dykstrom.jcc.basic.BasicTests.Companion.assertMessageContains
import se.dykstrom.jcc.basic.ast.statement.PrintStatement
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.Identifier

/**
 * Tests class `BasicSyntaxVisitor`, especially functionality related to FOR statements.
 *
 * @see BasicSyntaxVisitor
 */
class BasicSyntaxVisitorForTests : AbstractBasicSyntaxVisitorTests() {

    @Test
    fun shouldParseEmptyFor() {
        parseAndAssert("""
            FOR a% = 1 TO 5
            NEXT a%
            """,
            ForStatement(INE_I64_A, IL_1, IL_5, null, listOf())
        )
    }

    @Test
    fun shouldParseForWithStep() {
        parseAndAssert("""
            FOR a% = 5 TO 1 STEP -1
              PRINT a%
            NEXT
            """,
            ForStatement(INE_I64_A, IL_5, IL_1, IL_M1, listOf(PrintStatement(0, 0, listOf(IDE_I64_A))))
        )
    }

    @Test
    fun shouldParseForWithDefaultTypeCounter() {
        val counter = IdentifierNameExpression(0, 0, Identifier("i", F64.INSTANCE))
        parseAndAssert("FOR i = 1 TO 5: NEXT i", ForStatement(counter, IL_1, IL_5, null, listOf()))
    }

    @Test
    fun shouldParseOneLineFor() {
        val ps = PrintStatement(0, 0, listOf(IDE_I64_A))
        val fs = ForStatement(INE_I64_A, IL_1, IL_3, null, listOf(ps))
        parseAndAssert("FOR a% = 1 TO 3: PRINT a%: NEXT: PRINT 5", listOf(fs, PrintStatement(0, 0, listOf(IL_5))))
    }

    @Test
    fun shouldParseNestedFor() {
        val ps = PrintStatement(0, 0, listOf(IL_2))
        val inner = ForStatement(INE_I64_B, IL_1, IL_3, null, listOf(ps))
        val outer = ForStatement(INE_I64_A, IL_1, IL_5, null, listOf(inner, ps))
        parseAndAssert("""
            FOR a% = 1 TO 5
              FOR b% = 1 TO 3
                PRINT 2
              NEXT
              PRINT 2
            NEXT
            """,
            outer
        )
    }

    @Test
    fun shouldCloseTwoLoopsWithOneNext() {
        val ps = PrintStatement(0, 0, listOf(IL_2))
        val inner = ForStatement(INE_I64_B, IL_1, IL_3, null, listOf(ps))
        val outer = ForStatement(INE_I64_A, IL_1, IL_5, null, listOf(inner))
        parseAndAssert("""
            FOR a% = 1 TO 5
              FOR b% = 1 TO 3
                PRINT 2
            NEXT b%, a%
            PRINT 2
            """,
            listOf(outer, ps)
        )
    }

    @Test
    fun shouldKeepLabelsOnForAndNext() {
        val ps = LabelledStatement("20", PrintStatement(0, 0, listOf(IL_2)))
        val cs = LabelledStatement("30", CommentStatement(0, 0, "NEXT"))
        val fs = LabelledStatement("10", ForStatement(INE_I64_A, IL_1, IL_5, null, listOf(ps, cs)))
        parseAndAssert("""
            10 FOR a% = 1 TO 5
            20   PRINT 2
            30 NEXT
            """,
            fs
        )
    }

    @Test
    fun shouldParseForInsideWhileAndIf() {
        val ps = PrintStatement(0, 0, listOf(IL_2))
        val fs = ForStatement(INE_I64_A, IL_1, IL_5, null, listOf(ps))
        val ifs = IfStatement.builder(IDE_I64_A, listOf(fs)).build()
        val ws = WhileStatement(0, 0, IDE_I64_A, listOf(ifs))
        parseAndAssert("""
            WHILE a%
              IF a% THEN
                FOR a% = 1 TO 5
                  PRINT 2
                NEXT
              END IF
            WEND
            """,
            ws
        )
    }

    @Test
    fun shouldParseForInsideSingleLineIf() {
        val ps = PrintStatement(0, 0, listOf(IL_2))
        val fs = ForStatement(INE_I64_A, IL_1, IL_5, null, listOf(ps))
        val ifs = IfStatement.builder(IDE_I64_A, listOf(fs)).elseStatements(listOf(fs)).build()
        parseAndAssert("IF a% THEN FOR a% = 1 TO 5: PRINT 2: NEXT ELSE FOR a% = 1 TO 5: PRINT 2: NEXT", ifs)
    }

    @Test
    fun shouldParseExitFor() {
        val fs = ForStatement(INE_I64_A, IL_1, IL_5, null, listOf(LoopExitStatement(ForStatement::class.java)))
        parseAndAssert("""
            FOR a% = 1 TO 5
              EXIT FOR
            NEXT
            """,
            fs
        )
    }

    @Test
    fun shouldParseExitForInOneLineFor() {
        val ps = PrintStatement(0, 0, listOf(IDE_I64_A))
        val fs = ForStatement(INE_I64_A, IL_1, IL_3, null, listOf(ps, LoopExitStatement(ForStatement::class.java)))
        parseAndAssert("FOR a% = 1 TO 3: PRINT a%: EXIT FOR: NEXT", fs)
    }

    @Test
    fun shouldParseExitForInsideIf() {
        val ifs = IfStatement.builder(IDE_I64_A, listOf(LoopExitStatement(ForStatement::class.java))).build()
        val fs = ForStatement(INE_I64_A, IL_1, IL_5, null, listOf(ifs, ifs))
        parseAndAssert("""
            FOR a% = 1 TO 5
              IF a% THEN EXIT FOR
              IF a% THEN
                EXIT FOR
              END IF
            NEXT
            """,
            fs
        )
    }

    @Test
    fun shouldParseExitForInsideWhile() {
        val ws = WhileStatement(0, 0, IDE_I64_A, listOf(LoopExitStatement(ForStatement::class.java)))
        val fs = ForStatement(INE_I64_A, IL_1, IL_5, null, listOf(ws))
        parseAndAssert("FOR a% = 1 TO 5\nWHILE a%\nEXIT FOR\nWEND\nNEXT\n", fs)
    }

    @Test
    fun shouldReportForWithoutNext() {
        val errors = parseCollectingErrors("PRINT 1\nFOR a% = 1 TO 5\nPRINT a%\n")
        assertLines(errors, 2)
        assertMessageContains(errors, "FOR without NEXT")
    }

    @Test
    fun shouldReportNextWithoutFor() {
        val errors = parseCollectingErrors("PRINT 1\nNEXT a%\n")
        assertLines(errors, 2)
        assertMessageContains(errors, "NEXT without FOR")
    }

    @Test
    fun shouldReportNextWithTooManyCounters() {
        val errors = parseCollectingErrors("FOR a% = 1 TO 5\nNEXT a%, b%\n")
        assertLines(errors, 2)
        assertMessageContains(errors, "NEXT without FOR")
    }

    @Test
    fun shouldReportNextWithOtherCounter() {
        val errors = parseCollectingErrors("FOR a% = 1 TO 5\nPRINT a%\nNEXT b%\n")
        // The NEXT still closes the loop, so the FOR is not reported as well
        assertEquals(1, errors.size)
        assertLines(errors, 3)
        assertMessageContains(errors, "NEXT without FOR")
    }

    @Test
    fun shouldReportForClosedOutsideItsBlock() {
        val errors = parseCollectingErrors("IF a% THEN\nFOR b% = 1 TO 5\nEND IF\nNEXT b%\n")
        assertLines(errors, 2, 4)
        assertMessageContains(errors, "FOR without NEXT")
        assertMessageContains(errors, "NEXT without FOR")
    }

    @Test
    fun shouldBlameTheLoopANamedNextSkips() {
        val errors = parseCollectingErrors("FOR i = 1 TO 2\n  FOR j = 1 TO 2\n    PRINT j\nNEXT i\n")
        assertLines(errors, 2)
        assertMessageContains(errors, "FOR without NEXT")
    }

    @Test
    fun shouldBlameTheMiddleLoopANamedNextSkips() {
        val errors = parseCollectingErrors("FOR i = 1 TO 2\n  FOR j = 1 TO 2\n    FOR k = 1 TO 2\n    NEXT k\nNEXT i\n")
        assertLines(errors, 2)
        assertMessageContains(errors, "FOR without NEXT")
    }

    @Test
    fun shouldBlameTheInnerLoopWhenABareNextIsIndentedLikeTheOuter() {
        val errors = parseCollectingErrors("FOR i = 1 TO 2\n  FOR j = 1 TO 2\n    PRINT j\nNEXT\n")
        assertLines(errors, 2)
        assertMessageContains(errors, "FOR without NEXT")
    }

    @Test
    fun shouldBlameTheInnerLoopWhenLineNumbersAreUsed() {
        val errors = parseCollectingErrors("10 FOR i = 1 TO 2\n20   FOR j = 1 TO 2\n30     PRINT j\n40 NEXT\n")
        assertLines(errors, 2)
        assertMessageContains(errors, "FOR without NEXT")
    }

    @Test
    fun shouldBlameTheOpenLoopWhenNothingIsIndented() {
        val errors = parseCollectingErrors("FOR i = 1 TO 2\nFOR j = 1 TO 2\nPRINT j\nNEXT\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "FOR without NEXT")
    }

    @Test
    fun shouldNotBlameAnInnerOneLineLoop() {
        val errors = parseCollectingErrors("FOR i = 1 TO 2\n  FOR j = 1 TO 2: PRINT j: NEXT\nNEXT\n")
        assertEquals(listOf<Any>(), errors)
    }

    @Test
    fun shouldReportArrayElementAsCounter() {
        val errors = parseCollectingErrors("DIM a%(3)\nFOR a%(1) = 1 TO 3: NEXT\n")
        assertEquals(1, errors.size)
        assertLines(errors, 2)
        assertMessageContains(errors, "FOR index variable must be a simple variable, not an array element")
    }

    @Test
    fun shouldReportReservedWordAsCounter() {
        val errors = parseCollectingErrors("FOR next = 1 TO 3: NEXT\n")
        assertEquals(1, errors.size)
        assertMessageContains(errors, "'next' is a reserved word and cannot be used as a variable name")
    }
}
