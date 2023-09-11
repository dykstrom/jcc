/*
 * Copyright (C) 2023 Johan Dykstrom
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

package se.dykstrom.jcc.col.compiler

import org.junit.Test
import se.dykstrom.jcc.col.ast.AliasStatement
import se.dykstrom.jcc.col.ast.PrintlnStatement
import se.dykstrom.jcc.col.compiler.ColTests.Companion.verify
import se.dykstrom.jcc.col.types.NamedType
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral
import kotlin.test.assertTrue

class ColSyntaxParserTests : AbstractColSyntaxParserTests() {

    @Test
    fun shouldParseEmptyProgram() {
        // When
        val program = parse("")

        // Then
        assertTrue { program.statements.isEmpty() }
    }

    @Test
    fun shouldParseComment() {
        // When
        val program = parse("// comment")

        // Then
        assertTrue { program.statements.isEmpty() }
    }

    @Test
    fun shouldParseEmptyPrintln() {
        // Given
        val statement = PrintlnStatement(0, 0, null)

        // When
        val program = parse("println")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnAndComment() {
        // Given
        val statement = PrintlnStatement(0, 0, null)

        // When
        val program = parse("println // comment")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnLiteral() {
        // Given
        val statement = PrintlnStatement(0, 0, IntegerLiteral.ONE)

        // When
        val program = parse("println 1")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnAdd() {
        // Given
        val expression = AddExpression(0, 0, IntegerLiteral.ONE, IntegerLiteral.ZERO)
        val statement = PrintlnStatement(0, 0, expression)

        // When
        val program = parse("println 1 + 0")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseTwoStatements() {
        // Given
        val statement0 = PrintlnStatement(0, 0, IntegerLiteral.ZERO)
        val statement1 = PrintlnStatement(0, 0, IntegerLiteral.ONE)

        // When
        val program = parse(
            """
            println 0
            println 1
            """)

        // Then
        verify(program, statement0, statement1)
    }

    @Test
    fun shouldParseTwoStatementsWithoutArgs() {
        // Given
        val statement0 = PrintlnStatement(0, 0, null)
        val statement1 = PrintlnStatement(0, 0, null)

        // When
        val program = parse(
            """
            println
            println
            """)

        // Then
        verify(program, statement0, statement1)
    }

    @Test
    fun shouldParseAliasI64() {
        // Given
        val statement = AliasStatement(0, 0, "foo", NamedType("i64"))

        // When
        val program = parse("alias foo = i64")

        // Then
        verify(program, statement)
    }
}
