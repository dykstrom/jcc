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

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.antlr4.Antlr4Utils
import se.dykstrom.jcc.basic.type.BasicTypeManager
import se.dykstrom.jcc.common.ast.AstProgram
import se.dykstrom.jcc.common.error.CompilationError
import se.dykstrom.jcc.common.error.CompilationErrorListener

/**
 * Tests the mistakes `Basic.g4` accepts only so that [BasicSyntaxVisitor] can name them. The
 * grammar has to parse them: rejecting a mistake on a block header line costs the whole block,
 * because the parser gives up on the rule and orphans every terminator inside it.
 *
 * @author Johan Dykstrom
 */
class BasicSyntaxVisitorErrorTests {

    private val typeManager = BasicTypeManager()

    // ELSE IF written as two words:

    @Test
    fun shouldReportElseIfWrittenAsTwoWords() {
        val errors = parse(
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
        assertEquals(1, errors.size, "expected one error, got: ${errors.map { it.msg() }}")
        assertTrue(
            errors[0].msg().contains("'ELSE IF' is not 'ELSEIF'"),
            "Actual: '${errors[0].msg()}'"
        )
    }

    @Test
    fun shouldPointElseIfErrorAtTheElse() {
        // The ELSE is what has to change, so that is where the caret belongs
        val errors = parse("IF a THEN\n    PRINT 1\n    ELSE IF b THEN\n    PRINT 2\nEND IF\n")
        assertEquals(1, errors.size)
        assertEquals(3, errors[0].line())
        assertEquals(4, errors[0].column())
    }

    @Test
    fun shouldPointElseIfErrorAtTheElseAfterALineNumber() {
        val errors = parse("10 IF a THEN\n20 PRINT 1\n30 ELSE IF b THEN\n40 PRINT 2\n50 END IF\n")
        assertEquals(1, errors.size)
        assertEquals(3, errors[0].line())
        assertEquals(3, errors[0].column())
    }

    @Test
    fun shouldReportEveryElseIfWrittenAsTwoWords() {
        // visitIfThenBlock walks the ELSEIF blocks in reverse, so these reach the listener
        // bottom-up. CompilationMessage.compareTo is what puts them back in reading order, which
        // is the order they are printed in
        val errors = parse("IF a THEN\nPRINT 1\nELSE IF b THEN\nPRINT 2\nELSE IF c THEN\nPRINT 3\nEND IF\n")
        assertEquals(listOf(3, 5), errors.sorted().map { it.line() })
    }

    @Test
    fun shouldNotReportElseIfWrittenAsOneWord() {
        assertEquals(emptyList<CompilationError>(), parse("IF a THEN\nPRINT 1\nELSEIF b THEN\nPRINT 2\nEND IF\n"))
    }

    @Test
    fun shouldNotReportSingleLineIfWithElseIf() {
        // Valid QuickBASIC: an ELSE holding a single-line IF, not the two-word ELSEIF mistake
        assertEquals(emptyList<CompilationError>(), parse("IF a THEN PRINT 1 ELSE IF b THEN PRINT 2\n"))
    }

    @Test
    fun shouldNotReportNestedIfOnItsOwnLineInsideElse() {
        assertEquals(
            emptyList<CompilationError>(),
            parse("IF a THEN\nPRINT 1\nELSE\nIF b THEN\nPRINT 2\nEND IF\nEND IF\n")
        )
    }

    /**
     * Parses the given program text and builds the AST, returning the errors the visitor reported.
     * Any syntax error fails the test: these are mistakes the grammar is meant to accept.
     */
    private fun parse(text: String): List<CompilationError> {
        val errorListener = CompilationErrorListener()
        val baseErrorListener = Antlr4Utils.asBaseErrorListener(errorListener)

        val lexer = BasicLexer(CharStreams.fromString(text))
        lexer.removeErrorListeners()
        lexer.addErrorListener(baseErrorListener)

        val parser = BasicParser(CommonTokenStream(lexer))
        parser.removeErrorListeners()
        parser.addErrorListener(baseErrorListener)
        parser.errorHandler = BasicErrorStrategy()

        val ctx = parser.program()
        Antlr4Utils.checkParsingComplete(parser)
        assertEquals(
            0, parser.numberOfSyntaxErrors,
            "\nExpected the grammar to accept this, got: ${errorListener.errors}"
        )

        BasicSyntaxVisitor(typeManager, errorListener).visitProgram(ctx) as AstProgram
        return errorListener.errors
    }
}
