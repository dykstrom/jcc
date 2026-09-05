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
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_1
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_3
import se.dykstrom.jcc.basic.BasicTests.Companion.assertLines
import se.dykstrom.jcc.basic.BasicTests.Companion.assertMessageContains
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.AssignStatement
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.ast.IdentifierNameExpression
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.Identifier

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

    // Unsupported QuickBASIC statements:

    @Test
    fun shouldReportForNextLoop() {
        val errors = parseCollectingErrors("FOR i = 1 TO 10\nPRINT i\nNEXT i\n")
        // NEXT closes the loop the message already named, so it adds nothing of its own
        assertLines(errors, 1)
        assertMessageContains(errors, "'FOR ... NEXT' is not supported by JCC; use 'WHILE ... WEND'")
    }

    @Test
    fun shouldReportNextWithoutFor() {
        val errors = parseCollectingErrors("NEXT i\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'FOR ... NEXT' is not supported by JCC")
    }

    @Test
    fun shouldReportDoLoop() {
        val errors = parseCollectingErrors("i% = 0\nDO\nPRINT i%\nLOOP WHILE i% < 3\n")
        assertLines(errors, 2)
        assertMessageContains(errors, "'DO ... LOOP' is not supported by JCC; use 'WHILE ... WEND'")
    }

    @Test
    fun shouldReportSelectCase() {
        // Every CASE belongs to the block SELECT opened, and so does END SELECT
        val errors = parseCollectingErrors("SELECT CASE a%\nCASE 1\nPRINT 1\nCASE ELSE\nPRINT 2\nEND SELECT\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'SELECT CASE' is not supported by JCC; use 'IF ... ELSEIF ... END IF'")
    }

    @Test
    fun shouldReportSub() {
        val errors = parseCollectingErrors("SUB greet\nPRINT \"hi\"\nEND SUB\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'SUB' is not supported by JCC; use 'GOSUB ... RETURN'")
    }

    @Test
    fun shouldReportFunction() {
        val errors = parseCollectingErrors("FUNCTION twice(x)\ntwice = 2 * x\nEND FUNCTION\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'FUNCTION' is not supported by JCC; use 'DEF FN' to define a function")
    }

    @Test
    fun shouldReportType() {
        // The member declarations a TYPE block holds are not statements, and are not parsed;
        // the block header is what carries the message
        val errors = parseCollectingErrors("TYPE person\nEND TYPE\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'TYPE' is not supported by JCC")
    }

    @Test
    fun shouldReportExit() {
        val errors = parseCollectingErrors("EXIT FOR\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'EXIT' is not supported by JCC; use 'GOTO' to leave a loop")
    }

    @Test
    fun shouldReportPlainInput() {
        val errors = parseCollectingErrors("INPUT n%\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'INPUT' is not supported by JCC; use 'LINE INPUT'")
    }

    @Test
    fun shouldReportPrintUsing() {
        val errors = parseCollectingErrors("PRINT USING \"###\"; 42\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'PRINT USING' is not supported by JCC")
    }

    @Test
    fun shouldReportFileStatements() {
        val errors = parseCollectingErrors("OPEN \"f.txt\" FOR INPUT AS #1\nCLOSE #1\n")
        assertLines(errors, 1, 2)
        assertMessageContains(errors, "'OPEN' and 'CLOSE' are not supported by JCC; file I/O is not available")
    }

    @Test
    fun shouldReportScreenStatements() {
        val errors = parseCollectingErrors("LOCATE 1, 1\nCOLOR 7\n")
        assertLines(errors, 1, 2)
        assertMessageContains(errors, "'LOCATE' and 'COLOR' are not supported by JCC")
    }

    @Test
    fun shouldReportRedim() {
        val errors = parseCollectingErrors("REDIM a(20) AS INTEGER\n")
        assertLines(errors, 1)
        assertMessageContains(errors, "'REDIM' and 'ERASE' are not supported by JCC; arrays are static, use 'DIM'")
    }

    @Test
    fun shouldReportData() {
        val errors = parseCollectingErrors("DATA 1, 2, 3\nREAD a%\n")
        assertLines(errors, 1, 2)
        assertMessageContains(errors, "'DATA' and 'READ' are not supported by JCC; assign the values in code")
    }

    @Test
    fun shouldReportUnsupportedStatementAfterColon() {
        // The tail of an unsupported statement stops at COLON, so what follows it is still parsed
        val errors = parseCollectingErrors("PRINT 1 : LOCATE 1, 1 : PRINT 2\n")
        assertLines(errors, 1)
        assertEquals(10, errors[0].column())
    }

    @Test
    fun shouldReportEveryUnsupportedStatementInOneCompile() {
        // The parse succeeds, so one unsupported statement does not hide the next
        val errors = parseCollectingErrors("INPUT n%\nSELECT CASE n%\nEND SELECT\nLOCATE 1, 1\n")
        assertLines(errors, 1, 2, 4)
    }

    @Test
    fun shouldReportUnsupportedStatementInsideBlock() {
        val errors = parseCollectingErrors("IF a THEN\nFOR i = 1 TO 10\nNEXT i\nEND IF\n")
        assertLines(errors, 2)
    }

    // The keywords above are soft keywords, so they are still identifiers everywhere else:

    @Test
    fun shouldParseSoftKeywordAsVariableName() {
        val assignStatement = AssignStatement(0, 0, IdentifierNameExpression(0, 0, Identifier("data", F64.INSTANCE)), IL_3)
        parseAndAssert("data = 3", assignStatement)
    }

    @Test
    fun shouldParseSoftKeywordAsLabel() {
        val errors = parseCollectingErrors("next: GOTO next\n")
        assertEquals(emptyList<Any>(), errors)
    }

    @Test
    fun shouldParseSoftKeywordInExpression() {
        val assignStatement = AssignStatement(0, 0, IdentifierNameExpression(0, 0, Identifier("step", F64.INSTANCE)),
            AddExpression(0, 0, IdentifierDerefExpression(0, 0, Identifier("loop", F64.INSTANCE)), IL_1))
        parseAndAssert("step = loop + 1", assignStatement)
    }
}
