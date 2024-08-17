/*
 * Copyright (C) 2017 Johan Dykstrom
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
import se.dykstrom.jcc.antlr4.Antlr4Utils
import se.dykstrom.jcc.basic.BasicTests.Companion.ERROR_LISTENER
import se.dykstrom.jcc.basic.ast.PrintStatement
import se.dykstrom.jcc.common.ast.Expression
import se.dykstrom.jcc.common.ast.AstProgram
import se.dykstrom.jcc.common.ast.Statement

abstract class AbstractBasicSyntaxVisitorTests {

    private val typeManager = BasicTypeManager()

    /**
     * Tests the generic case of parsing code for printing one expression,
     * asserting that the parsed expression and the given expression are equal.
     *
     * @param text The expression in text form.
     * @param expectedExpression The expression in AST form.
     */
    protected fun testPrintOneExpression(text: String, expectedExpression: Expression) {
        val ps = PrintStatement(0, 0, listOf(expectedExpression))
        parseAndAssert("print $text", listOf(ps))
    }

    /**
     * Parses the given program text, and asserts that the parsed text and the given statements are equal.
     *
     * @param text The code in text form.
     * @param expectedStatements The code in AST form.
     */
    protected fun parseAndAssert(text: String, expectedStatements: List<Statement>) {
        val program = parse(text)
        val actualStatements = program.statements
        assertEquals(expectedStatements, actualStatements)
    }

    /**
     * Parses the given program text, and asserts that the parsed text and the given statement are equal.
     *
     * @param text The code in text form.
     * @param expectedStatement The code in AST form.
     */
    protected fun parseAndAssert(text: String, expectedStatement: Statement) {
        parseAndAssert(text, listOf(expectedStatement))
    }

    /**
     * Parses the given program text, and returns the AST for the parsed program.
     */
    protected fun parse(text: String): AstProgram {
        val lexer = BasicLexer(CharStreams.fromString(text))
        lexer.addErrorListener(ERROR_LISTENER)

        val parser = BasicParser(CommonTokenStream(lexer))
        parser.addErrorListener(ERROR_LISTENER)
        val ctx = parser.program()
        Antlr4Utils.checkParsingComplete(parser)

        val visitor = BasicSyntaxVisitor(typeManager)
        return visitor.visitProgram(ctx) as AstProgram
    }
}
