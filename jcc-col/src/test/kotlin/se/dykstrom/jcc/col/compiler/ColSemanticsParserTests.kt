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
import se.dykstrom.jcc.col.ast.statement.AliasStatement
import se.dykstrom.jcc.col.ast.statement.PrintlnStatement
import se.dykstrom.jcc.col.compiler.ColTests.Companion.CAST_0_I32
import se.dykstrom.jcc.col.compiler.ColTests.Companion.CAST_1_I32
import se.dykstrom.jcc.col.compiler.ColTests.Companion.CAST_5_I32
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FL_1_0
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_SUM1
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_17
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_18
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_5
import se.dykstrom.jcc.col.compiler.ColTests.Companion.verify
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.ast.BooleanLiteral.FALSE
import se.dykstrom.jcc.common.ast.BooleanLiteral.TRUE
import se.dykstrom.jcc.common.ast.IntegerLiteral.ONE
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import se.dykstrom.jcc.common.types.*

class ColSemanticsParserTests : AbstractColSemanticsParserTests() {

    @BeforeEach
    fun setUp() {
        symbolTable.addFunction(FUN_SUM1)
    }

    @Test
    fun shouldParseEmptyPrintln() {
        verify(parse("println"), PrintlnStatement(null))
    }

    @Test
    fun shouldParsePrintlnLiteral() {
        verify(parse("println 1"), PrintlnStatement(ONE))
    }

    @Test
    fun shouldPrintMaxI64() {
        // Given
        val statement = PrintlnStatement(IntegerLiteral(0, 0, "9223372036854775807"))

        // When
        val program = parse("println 9223372036854775807")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldPrintMaxI64WithUnderscores() {
        // Given
        val statement = PrintlnStatement(IntegerLiteral(0, 0, "9223372036854775807"))

        // When
        val program = parse("println 9_223_372_036_854_775_807")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldPrintMaxF64() {
        // Given
        val statement = PrintlnStatement(FloatLiteral(0, 0, "1.7976931348623157E+308"))

        // When
        val program = parse("println 1.7976931348623157E+308")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnLongExpression() {
        // Given
        val ae = AddExpression(ONE, ZERO)
        val ide = IDivExpression(IL_17, IL_5)
        val me = MulExpression(ide, IL_18)
        val se = SubExpression(ae, me)
        val statement = PrintlnStatement(se)

        // When
        val program = parse("println 1 + 0 - 17 div 5 * 18")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseArithmeticExpression() {
        // F64
        verify(parse("println 1.0 + 1.0"), PrintlnStatement(AddExpression(FL_1_0, FL_1_0)))
        verify(parse("println 1.0 - 1.0"), PrintlnStatement(SubExpression(FL_1_0, FL_1_0)))
        verify(parse("println 1.0 * 1.0"), PrintlnStatement(MulExpression(FL_1_0, FL_1_0)))
        verify(parse("println 1.0 / 1.0"), PrintlnStatement(DivExpression(FL_1_0, FL_1_0)))
        // I32
        verify(parse("println i32(1) + i32(0)"), PrintlnStatement(AddExpression(CAST_1_I32, CAST_0_I32)))
        verify(parse("println i32(1) - i32(0)"), PrintlnStatement(SubExpression(CAST_1_I32, CAST_0_I32)))
        verify(parse("println i32(1) * i32(0)"), PrintlnStatement(MulExpression(CAST_1_I32, CAST_0_I32)))
        verify(parse("println i32(1) div i32(0)"), PrintlnStatement(IDivExpression(CAST_1_I32, CAST_0_I32)))
        verify(parse("println i32(1) mod i32(0)"), PrintlnStatement(ModExpression(CAST_1_I32, CAST_0_I32)))
        // I64
        verify(parse("println 1 + 0"), PrintlnStatement(AddExpression(ONE, ZERO)))
        verify(parse("println 1 - 0"), PrintlnStatement(SubExpression(ONE, ZERO)))
        verify(parse("println 1 * 0"), PrintlnStatement(MulExpression(ONE, ZERO)))
        verify(parse("println 1 div 1"), PrintlnStatement(IDivExpression(ONE, ONE)))
        verify(parse("println 1 mod 1"), PrintlnStatement(ModExpression(ONE, ONE)))
    }

    @Test
    fun shouldParseBitwiseExpression() {
        // I32
        verify(parse("println i32(1) & i32(1)"), PrintlnStatement(AndExpression(CAST_1_I32, CAST_1_I32)))
        verify(parse("println i32(1) | i32(0)"), PrintlnStatement(OrExpression(CAST_1_I32, CAST_0_I32)))
        verify(parse("println i32(0) ^ i32(0)"), PrintlnStatement(XorExpression(CAST_0_I32, CAST_0_I32)))
        verify(parse("println ~i32(1)"), PrintlnStatement(NotExpression(CAST_1_I32)))
        // I64
        verify(parse("println 1 & 1"), PrintlnStatement(AndExpression(ONE, ONE)))
        verify(parse("println 1 | 0"), PrintlnStatement(OrExpression(ONE, ZERO)))
        verify(parse("println 0 ^ 0"), PrintlnStatement(XorExpression(ZERO, ZERO)))
        verify(parse("println ~1"), PrintlnStatement(NotExpression(ONE)))
    }

    @Test
    fun shouldParseLogicalExpression() {
        verify(parse("println true and false"), PrintlnStatement(LogicalAndExpression(0, 0, TRUE, FALSE)))
        verify(parse("println true or false"), PrintlnStatement(LogicalOrExpression(0, 0, TRUE, FALSE)))
        verify(parse("println true xor false"), PrintlnStatement(LogicalXorExpression(0, 0, TRUE, FALSE)))
        verify(parse("println not false"), PrintlnStatement(LogicalNotExpression(0, 0, FALSE)))
    }

    @Test
    fun shouldParseRelationalExpression() {
        // Bool
        val le = LessExpression(ONE, ONE)
        verify(parse("println (1 < 1) == (1 < 1)"), PrintlnStatement(EqualExpression(le, le)))
        verify(parse("println (1 < 1) != (1 < 1)"), PrintlnStatement(NotEqualExpression(le, le)))
        // F64
        verify(parse("println 1.0 == 1.0"), PrintlnStatement(EqualExpression(FL_1_0, FL_1_0)))
        verify(parse("println 1.0 != 1.0"), PrintlnStatement(NotEqualExpression(FL_1_0, FL_1_0)))
        verify(parse("println 1.0 < 1.0"), PrintlnStatement(LessExpression(FL_1_0, FL_1_0)))
        verify(parse("println 1.0 <= 1.0"), PrintlnStatement(LessOrEqualExpression(FL_1_0, FL_1_0)))
        verify(parse("println 1.0 > 1.0"), PrintlnStatement(GreaterExpression(FL_1_0, FL_1_0)))
        verify(parse("println 1.0 >= 1.0"), PrintlnStatement(GreaterOrEqualExpression(FL_1_0, FL_1_0)))
        // I32
        verify(parse("println i32(1) == i32(5)"), PrintlnStatement(EqualExpression(CAST_1_I32, CAST_5_I32)))
        verify(parse("println i32(1) != i32(5)"), PrintlnStatement(NotEqualExpression(CAST_1_I32, CAST_5_I32)))
        verify(parse("println i32(1) < i32(5)"), PrintlnStatement(LessExpression(CAST_1_I32, CAST_5_I32)))
        verify(parse("println i32(1) <= i32(5)"), PrintlnStatement(LessOrEqualExpression(CAST_1_I32, CAST_5_I32)))
        verify(parse("println i32(1) > i32(5)"), PrintlnStatement(GreaterExpression(CAST_1_I32, CAST_5_I32)))
        verify(parse("println i32(1) >= i32(5)"), PrintlnStatement(GreaterOrEqualExpression(CAST_1_I32, CAST_5_I32)))
        // I64
        verify(parse("println 1 == 5"), PrintlnStatement(EqualExpression(ONE, IL_5)))
        verify(parse("println 1 != 5"), PrintlnStatement(NotEqualExpression(ONE, IL_5)))
        verify(parse("println 1 < 5"), PrintlnStatement(LessExpression(ONE, IL_5)))
        verify(parse("println 1 <= 5"), PrintlnStatement(LessOrEqualExpression(ONE, IL_5)))
        verify(parse("println 1 > 5"), PrintlnStatement(GreaterExpression(ONE, IL_5)))
        verify(parse("println 1 >= 5"), PrintlnStatement(GreaterOrEqualExpression(ONE, IL_5)))
    }

    @Test
    fun shouldParseAlias() {
        verify(parse("alias foo as bool"), AliasStatement("foo", Bool.INSTANCE))
        verify(parse("alias bar as f64"), AliasStatement("bar", F64.INSTANCE))
        verify(parse("alias tee as i32"), AliasStatement("tee", I32.INSTANCE))
        verify(parse("alias moo as i64"), AliasStatement("moo", I64.INSTANCE))
    }

    @Test
    fun shouldParseAliasOfAlias() {
        // Given
        val as1 = AliasStatement("foo", I64.INSTANCE)
        val as2 = AliasStatement("bar", I64.INSTANCE)

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
        val statement = AliasStatement("foo", Fun.from(listOf(), I64.INSTANCE))

        // When
        val program = parse("alias foo as () -> i64")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseAliasFunctionTypeOneArg() {
        // Given
        val statement = AliasStatement("foo", Fun.from(listOf(F64.INSTANCE), I64.INSTANCE))

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
    fun shouldNotParseRedefineAlias() {
        parseAndExpectError("""
            alias foo as i64
            alias foo as f64
            """,
            "cannot redefine type: foo")
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
    fun shouldNotParseIDivFloatAndInt() {
        parseAndExpectError("println 1.0 div 5", "expected integer subexpressions")
    }

    @Test
    fun shouldNotParseIDivFloatAndFloat() {
        parseAndExpectError("println 1.0 div 5.0", "expected integer subexpressions")
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

    @Test
    fun shouldNotParseAddIntAndBool() {
        parseAndExpectError("println 1 + (3 == 2)", "cannot add i64 and bool")
    }

    @Test
    fun shouldNotParseSubIntAndFloat() {
        parseAndExpectError("println 1 - 1.0", "cannot subtract i64 and f64")
    }

    @Test
    fun shouldNotParseDivIntAndFloat() {
        parseAndExpectError("println 1 / 1.0", "cannot divide i64 and f64")
    }

    @Test
    fun shouldNotParseDivIntAndInt() {
        parseAndExpectError("println 1 / 1", "expected floating point subexpressions")
    }

    @Test
    fun shouldNotParseMulIntAndFloat() {
        parseAndExpectError("println 1 * 1.0", "cannot multiply i64 and f64")
    }

    @Test
    fun shouldNotParseMulI32AndFloat() {
        parseAndExpectError("println i32(1) * 1.0", "cannot multiply i32 and f64")
    }

    @Test
    fun shouldNotParseModIntAndFloat() {
        parseAndExpectError("println 1 mod 1.0", "cannot mod i64 and f64")
    }

    @Test
    fun shouldNotParseBitwiseAndFloat() {
        parseAndExpectError("println 1.0 & 5", "expected integer subexpressions")
    }

    @Test
    fun shouldNotParseBitwiseOrFloat() {
        parseAndExpectError("println 1.0 | 5", "expected integer subexpressions")
    }

    @Test
    fun shouldNotParseBitwiseXorFloat() {
        parseAndExpectError("println 1 ^ 5.0", "expected integer subexpressions")
    }

    @Test
    fun shouldNotParseBitwiseNotFloat() {
        parseAndExpectError("println ~1.0", "expected integer subexpression")
    }

    @Test
    fun shouldNotParseBitwiseNotBoolean() {
        parseAndExpectError("println ~true", "expected integer subexpression")
    }

    @Test
    fun shouldNotParseEqualIntAndFloat() {
        parseAndExpectError("println 10 == 7.5", "cannot compare i64 and f64")
    }

    @Test
    fun shouldNotParseGreaterThanFloatAndInt() {
        parseAndExpectError("println 10.0 > 7", "cannot compare f64 and i64")
    }

    @Test
    fun shouldNotParseLessThanBoolAndInt() {
        parseAndExpectError("println (10.0 == 3.0) < 7", "cannot compare bool and i64")
    }

    @Test
    fun shouldNotParseLessThanBoolAndBool() {
        parseAndExpectError("println (10.0 == 3.0) < (7 == 0)", "cannot compare bool and bool")
    }

    @Test
    fun shouldNotParseAddBoolAndBool() {
        parseAndExpectError("println (10.0 == 3.0) + (7 == 0)", "cannot add bool and bool")
        parseAndExpectError("println true + false", "cannot add bool and bool")
    }

    @Test
    fun shouldNotParseIDivBoolAndBool() {
        parseAndExpectError("println (10.0 == 3.0) div (7 == 0)", "expected integer subexpressions")
    }

    @Test
    fun shouldNotParseLogicalAndInt() {
        parseAndExpectError("println 1 and 2", "expected boolean subexpressions")
    }

    @Test
    fun shouldNotParseLogicalOrFloat() {
        parseAndExpectError("println 1.0 or 2.0", "expected boolean subexpressions")
    }

    @Test
    fun shouldNotParseLogicalXorFloat() {
        parseAndExpectError("println 1.0 xor 2.0", "expected boolean subexpressions")
    }

    @Test
    fun shouldNotParseLogicalNotFloat() {
        parseAndExpectError("println not 1.0", "expected boolean subexpression")
    }

    @Test
    fun shouldNotParseLogicalXorBoolAndFloat() {
        parseAndExpectError("println true xor 2.0", "cannot xor bool and f64")
    }
}
