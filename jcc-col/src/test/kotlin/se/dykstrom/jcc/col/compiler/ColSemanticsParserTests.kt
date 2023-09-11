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
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.types.I64

class ColSemanticsParserTests : AbstractColSemanticsParserTests() {

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
    fun shouldParsePrintlnLiteral() {
        // Given
        val statement = PrintlnStatement(0, 0, IntegerLiteral.ONE)

        // When
        val program = parse("println 1")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldPrintMaxI64() {
        // Given
        val statement = PrintlnStatement(0, 0, IntegerLiteral(0, 0, "9223372036854775807"))

        // When
        val program = parse("println 9223372036854775807")

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
    fun shouldParseAliasI64() {
        // Given
        val statement = AliasStatement(0, 0, "foo", I64.INSTANCE)

        // When
        val program = parse("alias foo = i64")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseAliasOfAlias() {
        // Given
        val as1 = AliasStatement(0, 0, "foo", I64.INSTANCE)
        val as2 = AliasStatement(0, 0, "bar", I64.INSTANCE)

        // When
        val program = parse(
            """
                // bar -> foo -> i64
                alias foo = i64
                alias bar = foo
                """
        )

        // Then
        verify(program, as1, as2)
    }

    @Test
    fun shouldNotParseUnknownAliasType() {
        parseAndExpectError("alias foo = bar", "undefined type: bar")
    }

    @Test
    fun shouldNotParseRedefineType() {
        parseAndExpectError("alias i64 = i64", "cannot redefine type: i64")
    }

    @Test
    fun shouldNotParseIntegerOverflow() {
        val value = "9223372036854775808"
        parseAndExpectError("println $value", "integer out of range: $value")
    }
}
