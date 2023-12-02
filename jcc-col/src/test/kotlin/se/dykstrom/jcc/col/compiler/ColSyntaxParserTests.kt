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
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FL_1_0
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_17
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_1_000
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_5
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_M_1
import se.dykstrom.jcc.col.compiler.ColTests.Companion.verify
import se.dykstrom.jcc.col.types.NamedType
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.ast.IntegerLiteral.ONE
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import kotlin.test.assertEquals
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
    fun shouldParsePrintlnIntegerLiteral() {
        // Given
        val statement = PrintlnStatement(0, 0, ONE)

        // When
        val program = parse("println 1")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnFloatLiteral() {
        // Given
        val statement = PrintlnStatement(0, 0, FL_1_0)

        // When
        val program = parse("println 1.0")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseFloatLiteralInDifferentFormats() {
        assertEquals("1.0", parseFloat("println 1.0"))
        assertEquals("17.0", parseFloat("println 17."))
        assertEquals("0.99", parseFloat("println .99"))
        assertEquals("1.0E+2", parseFloat("println 1E2"))
        assertEquals("5.678E+9", parseFloat("println 5.678E+9"))
        assertEquals("0.3E-10", parseFloat("println .3E-10"))
        assertEquals("1234.4567", parseFloat("println 1_234.456_7"))
    }

    @Test
    fun shouldParsePrintlnAdd() {
        // Given
        val expression = AddExpression(0, 0, ONE, ZERO)
        val statement = PrintlnStatement(0, 0, expression)

        // When
        val program = parse("println 1 + 0")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnSub() {
        // Given
        val expression = SubExpression(0, 0, ONE, ZERO)
        val statement = PrintlnStatement(0, 0, expression)

        // When
        val program = parse("println 1 - 0")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnMul() {
        // Given
        val expression = MulExpression(0, 0, ONE, ZERO)
        val statement = PrintlnStatement(0, 0, expression)

        // When
        val program = parse("println 1 * 0")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnFDiv() {
        // Given
        val expression = DivExpression(0, 0, ONE, FL_1_0)
        val statement = PrintlnStatement(0, 0, expression)

        // When
        val program = parse("println 1 / 1.0")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnIDiv() {
        // Given
        val expression = IDivExpression(0, 0, ONE, IL_5)
        val statement = PrintlnStatement(0, 0, expression)

        // When
        val program = parse("println 1 div 5")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnMod() {
        // Given
        val expression = ModExpression(0, 0, ONE, IL_5)
        val statement = PrintlnStatement(0, 0, expression)

        // When
        val program = parse("println 1 mod 5")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnNegatedInteger() {
        // Given
        val statement = PrintlnStatement(0, 0, IL_M_1)

        // When
        val program = parse("println -1")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnNegatedAdd() {
        // Given
        val ae = AddExpression(0, 0, IL_5, IL_17)
        val ne = NegateExpression(0, 0, ae)
        val statement = PrintlnStatement(0, 0, ne)

        // When
        val program = parse("println -(5 + 17)")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnDivSubMod() {
        // Given
        val de = IDivExpression(0, 0, IL_17, IL_5)
        val me = ModExpression(0, 0, IL_17, IL_5)
        val ae = AddExpression(0, 0, de, me)
        val statement = PrintlnStatement(0, 0, ae)

        // When
        val program = parse("println 17 div 5 + 17 mod 5")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseTwoStatements() {
        // Given
        val statement0 = PrintlnStatement(0, 0, ZERO)
        val statement1 = PrintlnStatement(0, 0, ONE)

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
    fun shouldParsePrintlnIntegerLiteralWithUnderscore() {
        // Given
        val statement = PrintlnStatement(0, 0, IL_1_000)

        // When
        val program = parse("println 1_000")

        // Then
        verify(program, statement)
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

    private fun parseFloat(text: String): String =
        ((parse(text).statements[0] as PrintlnStatement).expression() as FloatLiteral).value
}
