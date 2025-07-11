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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ast.statement.AliasStatement
import se.dykstrom.jcc.col.ast.statement.FunCallStatement
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FL_1_0
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_17
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_1_000
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_5
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_M_1
import se.dykstrom.jcc.col.compiler.ColTests.Companion.NT_BOOL
import se.dykstrom.jcc.col.compiler.ColTests.Companion.NT_F64
import se.dykstrom.jcc.col.compiler.ColTests.Companion.NT_I64
import se.dykstrom.jcc.col.compiler.ColTests.Companion.verify
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.ast.BooleanLiteral.FALSE
import se.dykstrom.jcc.common.ast.BooleanLiteral.TRUE
import se.dykstrom.jcc.common.ast.IntegerLiteral.ONE
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import se.dykstrom.jcc.common.types.Fun

class ColSyntaxParserTests : AbstractColSyntaxParserTests() {

    @Test
    fun shouldParseEmptyProgram() {
        assertTrue { parse("").statements.isEmpty() }
    }

    @Test
    fun shouldParseComment() {
        assertTrue { parse("// comment").statements.isEmpty() }
    }

    @Test
    fun shouldParseEmptyPrintln() {
        verify(parse("call println()"), printlnCall())
    }

    @Test
    fun shouldParsePrintlnAndComment() {
        verify(parse("call println() // comment"), printlnCall())
    }

    @Test
    fun shouldParseLiterals() {
        // Decimal
        verify(parse("call println(1)"), printlnCall(ONE))
        verify(parse("call println(-1)"), printlnCall(IL_M_1))
        verify(parse("call println(1_000)"), printlnCall(IL_1_000))

        // Binary
        verify(parse("call println(0b1)"), printlnCall(ONE))
        verify(parse("call println(0b101)"), printlnCall(IL_5))

        // Hexadecimal
        verify(parse("call println(0x05)"), printlnCall(IL_5))
        verify(parse("call println(0x11)"), printlnCall(IL_17))

        // Floating point
        verify(parse("call println(1.0)"), printlnCall(FL_1_0))

        // Boolean
        verify(parse("call println(true)"), printlnCall(TRUE))
        verify(parse("call println(false)"), printlnCall(FALSE))
    }

    @Test
    fun shouldParseFloatLiteralInDifferentFormats() {
        assertEquals("1.0", extractFloat("call println(1.0)"))
        assertEquals("17.0", extractFloat("call println(17.)"))
        assertEquals("0.99", extractFloat("call println(.99)"))
        assertEquals("1.0E+2", extractFloat("call println(1E2)"))
        assertEquals("5.678E+9", extractFloat("call println(5.678E+9)"))
        assertEquals("0.3E-10", extractFloat("call println(.3E-10)"))
        assertEquals("1234.4567", extractFloat("call println(1_234.456_7)"))
    }

    @Test
    fun shouldParseArithmeticExpression() {
        verify(parse("call println(1 + 0)"), printlnCall(AddExpression(0, 0, ONE, ZERO)))
        verify(parse("call println(1 - 0)"), printlnCall(SubExpression(0, 0, ONE, ZERO)))
        verify(parse("call println(1 * 0)"), printlnCall(MulExpression(0, 0, ONE, ZERO)))
        verify(parse("call println(1 / 1.0)"), printlnCall(DivExpression(0, 0, ONE, FL_1_0)))
        verify(parse("call println(1 div 5)"), printlnCall(IDivExpression(0, 0, ONE, IL_5)))
        verify(parse("call println(1 mod 5)"), printlnCall(ModExpression(0, 0, ONE, IL_5)))
    }

    @Test
    fun shouldParseNegatedAdd() {
        // Given
        val ae = AddExpression(0, 0, IL_5, IL_17)
        val ne = NegateExpression(0, 0, ae)
        val statement = printlnCall(ne)

        // When
        val program = parse("call println(-(5 + 17))")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseDivSubMod() {
        // Given
        val de = IDivExpression(0, 0, IL_17, IL_5)
        val me = ModExpression(0, 0, IL_17, IL_5)
        val ae = AddExpression(0, 0, de, me)
        val statement = printlnCall(ae)

        // When
        val program = parse("call println(17 div 5 + 17 mod 5)")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseBitwiseExpression() {
        verify(parse("call println(1 & 5)"), printlnCall(AndExpression(ONE, IL_5)))
        verify(parse("call println(1 | 5)"), printlnCall(OrExpression(ONE, IL_5)))
        verify(parse("call println(1 ^ 5)"), printlnCall(XorExpression(ONE, IL_5)))
        verify(parse("call println(~5)"), printlnCall(NotExpression(IL_5)))
        verify(parse("call println(~5 | 17)"), printlnCall(OrExpression(NotExpression(IL_5), IL_17)))
        verify(parse("call println(5 | 17 & 1)"), printlnCall(OrExpression(IL_5, AndExpression(IL_17, ONE))))
    }

    @Test
    fun shouldParseRelationalExpression() {
        verify(parse("call println(1 == 5)"), printlnCall(EqualExpression(ONE, IL_5)))
        verify(parse("call println(1 != 5)"), printlnCall(NotEqualExpression(ONE, IL_5)))
        verify(parse("call println(1 < 5)"), printlnCall(LessExpression(ONE, IL_5)))
        verify(parse("call println(1 <= 5)"), printlnCall(LessOrEqualExpression(ONE, IL_5)))
        verify(parse("call println(1 > 5)"), printlnCall(GreaterExpression(0, 0, ONE, IL_5)))
        verify(parse("call println(1 >= 5)"), printlnCall(GreaterOrEqualExpression(0, 0, ONE, IL_5)))
    }

    @Test
    fun shouldParseLogicalExpression() {
        verify(parse("call println(true and false)"), printlnCall(LogicalAndExpression(0, 0, TRUE, FALSE)))
        verify(parse("call println(true or false)"), printlnCall(LogicalOrExpression(0, 0, TRUE, FALSE)))
        verify(parse("call println(true xor false)"), printlnCall(LogicalXorExpression(0, 0, TRUE, FALSE)))
        verify(parse("call println(not true)"), printlnCall(LogicalNotExpression(0, 0, TRUE)))
    }

    @Test
    fun shouldParseTwoStatements() {
        // Given
        val statement0 = printlnCall(ZERO)
        val statement1 = printlnCall(ONE)

        // When
        val program = parse(
            """
            call println(0)
            call println(1)
            """)

        // Then
        verify(program, statement0, statement1)
    }

    @Test
    fun shouldParseTwoStatementsWithoutArgs() {
        // Given
        val statement0 = printlnCall()
        val statement1 = printlnCall()

        // When
        val program = parse(
            """
            call println()
            call println()
            """)

        // Then
        verify(program, statement0, statement1)
    }

    @Test
    fun shouldParseAliasI64() {
        // Given
        val statement = AliasStatement("foo", NT_I64)

        // When
        val program = parse("alias foo as i64")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseAliasBool() {
        // Given
        val statement = AliasStatement("foo", NT_BOOL)

        // When
        val program = parse("alias foo as bool")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseAliasFunctionTypeNoArgs() {
        // Given
        val statement = AliasStatement("foo", Fun.from(listOf(), NT_I64))

        // When
        val program = parse("alias foo as () -> i64")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseAliasFunctionTypeOneArg() {
        // Given
        val statement = AliasStatement("foo", Fun.from(listOf(NT_I64), NT_I64))

        // When
        val program = parse("alias foo as (i64) -> i64")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseAliasFunctionTypeTwoArgs() {
        // Given
        val statement = AliasStatement("foo", Fun.from(listOf(NT_F64, NT_I64), NT_F64))

        // When
        val program = parse("alias foo as (f64, i64) -> f64")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseAliasFunctionTypeWithFunctionTypeReturn() {
        // Given
        val type = Fun.from(listOf(NT_I64), Fun.from(listOf(NT_F64), NT_F64))
        val statement = AliasStatement("foo", type)

        // When
        val program = parse("alias foo as (i64) -> (f64) -> f64")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseAliasFunctionTypeWithFunctionTypeArg() {
        // Given
        val type = Fun.from(listOf(Fun.from(listOf(NT_F64), NT_F64)), NT_I64)
        val statement = AliasStatement("foo", type)

        // When
        val program = parse("alias foo as ((f64) -> f64) -> i64")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseAliasFunctionTypeWithSeveralFunctionTypes() {
        // Given
        val argTypes = listOf(NT_I64, Fun.from(listOf(NT_F64), NT_F64), NT_I64)
        val returnType = Fun.from(listOf(NT_I64), NT_F64)
        val type = Fun.from(argTypes, returnType)
        val statement = AliasStatement("foo", type)

        // When
        val program = parse("alias foo as (i64, (f64) -> f64, i64) -> (i64) -> f64")

        // Then
        verify(program, statement)
    }

    private fun extractFloat(text: String): String =
        ((parse(text).statements[0] as FunCallStatement).expression().args[0] as FloatLiteral).value
}
