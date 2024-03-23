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

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ast.AliasStatement
import se.dykstrom.jcc.col.ast.PrintlnStatement
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FL_1_0
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_SUM1
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_17
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_18
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_5
import se.dykstrom.jcc.col.compiler.ColTests.Companion.verify
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.ast.IntegerLiteral.ONE
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_FMOD
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.I64

class ColSemanticsParserTests : AbstractColSemanticsParserTests() {

    @BeforeEach
    fun setUp() {
        // Function fmod is used for modulo operations on floats
        symbolTable.addFunction(FUN_FMOD)
        symbolTable.addFunction(FUN_SUM1)
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
    fun shouldParsePrintlnLiteral() {
        // Given
        val statement = PrintlnStatement(0, 0, ONE)

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
    fun shouldPrintMaxI64WithUnderscores() {
        // Given
        val statement = PrintlnStatement(0, 0, IntegerLiteral(0, 0, "9223372036854775807"))

        // When
        val program = parse("println 9_223_372_036_854_775_807")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldPrintMaxF64() {
        // Given
        val statement = PrintlnStatement(0, 0, FloatLiteral(0, 0, "1.7976931348623157E+308"))

        // When
        val program = parse("println 1.7976931348623157E+308")

        // Then
        verify(program, statement)
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
    fun shouldParsePrintlnLongExpression() {
        // Given
        val ae = AddExpression(0, 0, ONE, ZERO)
        val ide = IDivExpression(0, 0, IL_17, IL_5)
        val me = MulExpression(0, 0, ide, IL_18)
        val de = DivExpression(0, 0, me, ONE)
        val se = SubExpression(0, 0, ae, de)
        val statement = PrintlnStatement(0, 0, se)

        // When
        val program = parse("println 1 + 0 - 17 div 5 * 18 / 1")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnModIntegers() {
        // Given
        val expression = ModExpression(0, 0, ONE, IL_17)
        val statement = PrintlnStatement(0, 0, expression)

        // When
        val program = parse("println 1 mod 17")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnModFloats() {
        // Given
        val expression = FunctionCallExpression(0, 0, FUN_FMOD.identifier, listOf(ONE, FL_1_0))
        val statement = PrintlnStatement(0, 0, expression)

        // When
        val program = parse("println 1 mod 1.0")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseAliasI64() {
        // Given
        val statement = AliasStatement(0, 0, "foo", I64.INSTANCE)

        // When
        val program = parse("alias foo as i64")

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
                alias foo as i64
                alias bar as foo
                """
        )

        // Then
        verify(program, as1, as2)
    }

    @Test
    fun shouldParseAliasFunctionTypeNoArgs() {
        // Given
        val statement = AliasStatement(0, 0, "foo", Fun.from(listOf(), I64.INSTANCE))

        // When
        val program = parse("alias foo as () -> i64")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseAliasFunctionTypeOneArg() {
        // Given
        val statement = AliasStatement(0, 0, "foo", Fun.from(listOf(F64.INSTANCE), I64.INSTANCE))

        // When
        val program = parse("alias foo as (f64) -> i64")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldNotParseUnknownAliasType() {
        parseAndExpectError("alias foo as bar", "undefined type: bar")
    }

    @Test
    fun shouldNotParseRedefineType() {
        parseAndExpectError("alias i64 as i64", "cannot redefine type: i64")
    }

    @Test
    fun shouldNotParseIntegerOverflow() {
        val value = "9223372036854775808"
        parseAndExpectError("println $value", "integer out of range: $value")
    }

    @Test
    fun shouldNotParseFloatOverflow() {
        val value = "1.7976931348623157E+309"
        parseAndExpectError("println $value", "float out of range: $value")
    }

    @Test
    fun shouldNotParseIDivFloat() {
        parseAndExpectError("println 1.0 div 5", "expected integer subexpressions")
    }

    @Test
    fun shouldNotParseIDivByZero() {
        parseAndExpectError("println 1 div 0", "division by zero")
    }

    @Test
    fun shouldNotParseModuloByZero() {
        parseAndExpectError("println 1 mod 0", "division by zero")
    }

    @Test
    fun shouldNotParseFloatModuloByZero() {
        parseAndExpectError("println 1 mod 0.0", "division by zero")
    }

    @Test
    fun shouldNotParseModuloByZeroInFunctionCall() {
        parseAndExpectError("println sum(1 mod 0)", "division by zero")
    }
}
