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
import se.dykstrom.jcc.col.ast.statement.PrintlnStatement
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
        verify(parse("println"), PrintlnStatement(null))
    }

    @Test
    fun shouldParsePrintlnAndComment() {
        verify(parse("println // comment"), PrintlnStatement(null))
    }

    @Test
    fun shouldParseLiterals() {
        verify(parse("println 1"), PrintlnStatement(ONE))
        verify(parse("println -1"), PrintlnStatement(IL_M_1))
        verify(parse("println 1.0"), PrintlnStatement(FL_1_0))
        verify(parse("println true"), PrintlnStatement(TRUE))
        verify(parse("println false"), PrintlnStatement(FALSE))
    }

    @Test
    fun shouldParseFloatLiteralInDifferentFormats() {
        assertEquals("1.0", extractFloat("println 1.0"))
        assertEquals("17.0", extractFloat("println 17."))
        assertEquals("0.99", extractFloat("println .99"))
        assertEquals("1.0E+2", extractFloat("println 1E2"))
        assertEquals("5.678E+9", extractFloat("println 5.678E+9"))
        assertEquals("0.3E-10", extractFloat("println .3E-10"))
        assertEquals("1234.4567", extractFloat("println 1_234.456_7"))
    }

    @Test
    fun shouldParseArithmeticExpression() {
        verify(parse("println 1 + 0"), PrintlnStatement(AddExpression(0, 0, ONE, ZERO)))
        verify(parse("println 1 - 0"), PrintlnStatement(SubExpression(0, 0, ONE, ZERO)))
        verify(parse("println 1 * 0"), PrintlnStatement(MulExpression(0, 0, ONE, ZERO)))
        verify(parse("println 1 / 1.0"), PrintlnStatement(DivExpression(0, 0, ONE, FL_1_0)))
        verify(parse("println 1 div 5"), PrintlnStatement(IDivExpression(0, 0, ONE, IL_5)))
        verify(parse("println 1 mod 5"), PrintlnStatement(ModExpression(0, 0, ONE, IL_5)))
    }

    @Test
    fun shouldParseNegatedInteger() {
        // Given
        val statement = PrintlnStatement(IL_M_1)

        // When
        val program = parse("println -1")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseNegatedAdd() {
        // Given
        val ae = AddExpression(0, 0, IL_5, IL_17)
        val ne = NegateExpression(0, 0, ae)
        val statement = PrintlnStatement(ne)

        // When
        val program = parse("println -(5 + 17)")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseDivSubMod() {
        // Given
        val de = IDivExpression(0, 0, IL_17, IL_5)
        val me = ModExpression(0, 0, IL_17, IL_5)
        val ae = AddExpression(0, 0, de, me)
        val statement = PrintlnStatement(ae)

        // When
        val program = parse("println 17 div 5 + 17 mod 5")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseBitwiseExpression() {
        verify(parse("println 1 & 5"), PrintlnStatement(AndExpression(ONE, IL_5)))
        verify(parse("println 1 | 5"), PrintlnStatement(OrExpression(ONE, IL_5)))
        verify(parse("println 1 ^ 5"), PrintlnStatement(XorExpression(ONE, IL_5)))
        verify(parse("println ~5"), PrintlnStatement(NotExpression(IL_5)))
        verify(parse("println ~5 | 17"), PrintlnStatement(OrExpression(NotExpression(IL_5), IL_17)))
        verify(parse("println 5 | 17 & 1"), PrintlnStatement(OrExpression(IL_5, AndExpression(IL_17, ONE))))
    }

    @Test
    fun shouldParseRelationalExpression() {
        verify(parse("println 1 == 5"), PrintlnStatement(EqualExpression(ONE, IL_5)))
        verify(parse("println 1 != 5"), PrintlnStatement(NotEqualExpression(ONE, IL_5)))
        verify(parse("println 1 < 5"), PrintlnStatement(LessExpression(ONE, IL_5)))
        verify(parse("println 1 <= 5"), PrintlnStatement(LessOrEqualExpression(ONE, IL_5)))
        verify(parse("println 1 > 5"), PrintlnStatement(GreaterExpression(0, 0, ONE, IL_5)))
        verify(parse("println 1 >= 5"), PrintlnStatement(GreaterOrEqualExpression(0, 0, ONE, IL_5)))
    }

    @Test
    fun shouldParseLogicalExpression() {
        verify(parse("println true and false"), PrintlnStatement(LogicalAndExpression(0, 0, TRUE, FALSE)))
        verify(parse("println true or false"), PrintlnStatement(LogicalOrExpression(0, 0, TRUE, FALSE)))
        verify(parse("println true xor false"), PrintlnStatement(LogicalXorExpression(0, 0, TRUE, FALSE)))
        verify(parse("println not true"), PrintlnStatement(LogicalNotExpression(0, 0, TRUE)))
    }

    @Test
    fun shouldParseTwoStatements() {
        // Given
        val statement0 = PrintlnStatement(ZERO)
        val statement1 = PrintlnStatement(ONE)

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
        val statement0 = PrintlnStatement(null)
        val statement1 = PrintlnStatement(null)

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
        val statement = PrintlnStatement(IL_1_000)

        // When
        val program = parse("println 1_000")

        // Then
        verify(program, statement)
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
        ((parse(text).statements[0] as PrintlnStatement).expression() as FloatLiteral).value
}
