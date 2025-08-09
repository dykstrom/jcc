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
import se.dykstrom.jcc.col.compiler.ColSymbols.*
import se.dykstrom.jcc.col.compiler.ColTests.Companion.CAST_0_I32
import se.dykstrom.jcc.col.compiler.ColTests.Companion.CAST_1_0_F32
import se.dykstrom.jcc.col.compiler.ColTests.Companion.CAST_1_I32
import se.dykstrom.jcc.col.compiler.ColTests.Companion.CAST_5_I32
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FL_1_0
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_PRINTLN
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
        symbolTable.addFunction(FUN_PRINTLN)
        symbolTable.addFunction(FUN_SUM1)
    }

    @Test
    fun shouldParseEmptyPrintln() {
        verify(parse("call println()"), funCall(FUN_PRINTLN))
    }

    @Test
    fun shouldParsePrintlnLiteral() {
        verify(parse("call println(1)"), funCall(BF_PRINTLN_I64, ONE))
    }

    @Test
    fun shouldPrintMaxI64() {
        // Given
        val statement = funCall(BF_PRINTLN_I64, IntegerLiteral(0, 0, "9223372036854775807"))

        // When
        val program = parse("call println(9223372036854775807)")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldPrintMaxI64WithUnderscores() {
        // Given
        val statement = funCall(BF_PRINTLN_I64, IntegerLiteral(0, 0, "9223372036854775807"))

        // When
        val program = parse("call println(9_223_372_036_854_775_807)")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldPrintMaxF64() {
        // Given
        val statement = funCall(BF_PRINTLN_F64, FloatLiteral(0, 0, "1.7976931348623157E+308"))

        // When
        val program = parse("call println(1.7976931348623157E+308)")

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
        val statement = funCall(BF_PRINTLN_I64, se)

        // When
        val program = parse("call println(1 + 0 - 17 div 5 * 18)")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseArithmeticExpression() {
        // F64
        verify(parse("call println(1.0 + 1.0)"), funCall(BF_PRINTLN_F64, AddExpression(FL_1_0, FL_1_0)))
        verify(parse("call println(1.0 - 1.0)"), funCall(BF_PRINTLN_F64, SubExpression(FL_1_0, FL_1_0)))
        verify(parse("call println(1.0 * 1.0)"), funCall(BF_PRINTLN_F64, MulExpression(FL_1_0, FL_1_0)))
        verify(parse("call println(1.0 / 1.0)"), funCall(BF_PRINTLN_F64, DivExpression(FL_1_0, FL_1_0)))
        // I32
        verify(parse("call println(i32(1) + i32(0))"), funCall(BF_PRINTLN_I32, AddExpression(CAST_1_I32, CAST_0_I32)))
        verify(parse("call println(i32(1) - i32(0))"), funCall(BF_PRINTLN_I32, SubExpression(CAST_1_I32, CAST_0_I32)))
        verify(parse("call println(i32(1) * i32(0))"), funCall(BF_PRINTLN_I32, MulExpression(CAST_1_I32, CAST_0_I32)))
        verify(parse("call println(i32(1) div i32(0))"), funCall(BF_PRINTLN_I32, IDivExpression(CAST_1_I32, CAST_0_I32)))
        verify(parse("call println(i32(1) mod i32(0))"), funCall(BF_PRINTLN_I32, ModExpression(CAST_1_I32, CAST_0_I32)))
        // I64
        verify(parse("call println(1 + 0)"), funCall(BF_PRINTLN_I64, AddExpression(ONE, ZERO)))
        verify(parse("call println(1 - 0)"), funCall(BF_PRINTLN_I64, SubExpression(ONE, ZERO)))
        verify(parse("call println(1 * 0)"), funCall(BF_PRINTLN_I64, MulExpression(ONE, ZERO)))
        verify(parse("call println(1 div 1)"), funCall(BF_PRINTLN_I64, IDivExpression(ONE, ONE)))
        verify(parse("call println(1 mod 1)"), funCall(BF_PRINTLN_I64, ModExpression(ONE, ONE)))
    }

    @Test
    fun shouldParseBitwiseExpression() {
        // I32
        verify(parse("call println(i32(1) & i32(1))"), funCall(BF_PRINTLN_I32, AndExpression(CAST_1_I32, CAST_1_I32)))
        verify(parse("call println(i32(1) | i32(0))"), funCall(BF_PRINTLN_I32, OrExpression(CAST_1_I32, CAST_0_I32)))
        verify(parse("call println(i32(0) ^ i32(0))"), funCall(BF_PRINTLN_I32, XorExpression(CAST_0_I32, CAST_0_I32)))
        verify(parse("call println(~i32(1))"), funCall(BF_PRINTLN_I32, NotExpression(CAST_1_I32)))
        // I64
        verify(parse("call println(1 & 1)"), funCall(BF_PRINTLN_I64, AndExpression(ONE, ONE)))
        verify(parse("call println(1 | 0)"), funCall(BF_PRINTLN_I64, OrExpression(ONE, ZERO)))
        verify(parse("call println(0 ^ 0)"), funCall(BF_PRINTLN_I64, XorExpression(ZERO, ZERO)))
        verify(parse("call println(~1)"), funCall(BF_PRINTLN_I64, NotExpression(ONE)))
    }

    @Test
    fun shouldParseLogicalExpression() {
        verify(parse("call println(true and false)"), funCall(BF_PRINTLN_BOOL, LogicalAndExpression(0, 0, TRUE, FALSE)))
        verify(parse("call println(true or false)"), funCall(BF_PRINTLN_BOOL, LogicalOrExpression(0, 0, TRUE, FALSE)))
        verify(parse("call println(true xor false)"), funCall(BF_PRINTLN_BOOL, LogicalXorExpression(0, 0, TRUE, FALSE)))
        verify(parse("call println(not false)"), funCall(BF_PRINTLN_BOOL, LogicalNotExpression(0, 0, FALSE)))
    }

    @Test
    fun shouldParseRelationalExpression() {
        // Bool
        val le = LessExpression(ONE, ONE)
        verify(parse("call println((1 < 1) == (1 < 1))"), funCall(BF_PRINTLN_BOOL, EqualExpression(le, le)))
        verify(parse("call println((1 < 1) != (1 < 1))"), funCall(BF_PRINTLN_BOOL, NotEqualExpression(le, le)))
        // F64
        verify(parse("call println(1.0 == 1.0)"), funCall(BF_PRINTLN_BOOL, EqualExpression(FL_1_0, FL_1_0)))
        verify(parse("call println(1.0 != 1.0)"), funCall(BF_PRINTLN_BOOL, NotEqualExpression(FL_1_0, FL_1_0)))
        verify(parse("call println(1.0 < 1.0)"), funCall(BF_PRINTLN_BOOL, LessExpression(FL_1_0, FL_1_0)))
        verify(parse("call println(1.0 <= 1.0)"), funCall(BF_PRINTLN_BOOL, LessOrEqualExpression(FL_1_0, FL_1_0)))
        verify(parse("call println(1.0 > 1.0)"), funCall(BF_PRINTLN_BOOL, GreaterExpression(FL_1_0, FL_1_0)))
        verify(parse("call println(1.0 >= 1.0)"), funCall(BF_PRINTLN_BOOL, GreaterOrEqualExpression(FL_1_0, FL_1_0)))
        // I32
        verify(parse("call println(i32(1) == i32(5))"), funCall(BF_PRINTLN_BOOL, EqualExpression(CAST_1_I32, CAST_5_I32)))
        verify(parse("call println(i32(1) != i32(5))"), funCall(BF_PRINTLN_BOOL, NotEqualExpression(CAST_1_I32, CAST_5_I32)))
        verify(parse("call println(i32(1) < i32(5))"), funCall(BF_PRINTLN_BOOL, LessExpression(CAST_1_I32, CAST_5_I32)))
        verify(parse("call println(i32(1) <= i32(5))"), funCall(BF_PRINTLN_BOOL, LessOrEqualExpression(CAST_1_I32, CAST_5_I32)))
        verify(parse("call println(i32(1) > i32(5))"), funCall(BF_PRINTLN_BOOL, GreaterExpression(CAST_1_I32, CAST_5_I32)))
        verify(parse("call println(i32(1) >= i32(5))"), funCall(BF_PRINTLN_BOOL, GreaterOrEqualExpression(CAST_1_I32, CAST_5_I32)))
        // I64
        verify(parse("call println(1 == 5)"), funCall(BF_PRINTLN_BOOL, EqualExpression(ONE, IL_5)))
        verify(parse("call println(1 != 5)"), funCall(BF_PRINTLN_BOOL, NotEqualExpression(ONE, IL_5)))
        verify(parse("call println(1 < 5)"), funCall(BF_PRINTLN_BOOL, LessExpression(ONE, IL_5)))
        verify(parse("call println(1 <= 5)"), funCall(BF_PRINTLN_BOOL, LessOrEqualExpression(ONE, IL_5)))
        verify(parse("call println(1 > 5)"), funCall(BF_PRINTLN_BOOL, GreaterExpression(ONE, IL_5)))
        verify(parse("call println(1 >= 5)"), funCall(BF_PRINTLN_BOOL, GreaterOrEqualExpression(ONE, IL_5)))
    }

    @Test
    fun shouldParsePrintlnIfExpression() {
        // Given
        val ide = IDivExpression(IL_17, IL_5)
        val ae = AddExpression(ONE, ZERO)
        val ne = NotEqualExpression(IL_5, ZERO)
        val ie = IfExpression(ne, ide, ae)
        val statement = funCall(BF_PRINTLN_I64, ie)

        // When
        val program = parse("call println(if 5 != 0 then 17 div 5 else 1 + 0)")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnIfExpressionWithCast() {
        // Given
        val ne = NotEqualExpression(IL_5, ZERO)
        val ie = IfExpression(ne, CastToFloatExpression(CAST_1_0_F32, F64.INSTANCE), FL_1_0)
        val statement = funCall(BF_PRINTLN_F64, ie)

        // When
        val program = parse("call println(if 5 != 0 then f32(1.0) else 1.0)")

        // Then
        verify(program, statement)
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
        parseAndExpectError("call println($value)", "integer out of range: $value")
    }

    @Test
    fun shouldNotParseFloatOverflow() {
        val value = "1.7976931348623157E+309"
        parseAndExpectError("call println($value)", "float out of range: $value")
    }

    @Test
    fun shouldNotParseIDivFloatAndInt() {
        parseAndExpectError("call println(1.0 div 5)", "expected integer subexpressions")
    }

    @Test
    fun shouldNotParseIDivFloatAndFloat() {
        parseAndExpectError("call println(1.0 div 5.0)", "expected integer subexpressions")
    }

    @Test
    fun shouldNotParseIDivByZero() {
        parseAndExpectError("call println(1 div 0)", "division by zero")
    }

    @Test
    fun shouldNotParseModuloByZero() {
        parseAndExpectError("call println(1 mod 0)", "division by zero")
    }

    @Test
    fun shouldNotParseFloatModuloByZero() {
        parseAndExpectError("call println(1 mod 0.0)", "division by zero")
    }

    @Test
    fun shouldNotParseModuloByZeroInFunctionCall() {
        parseAndExpectError("call println(sum(1 mod 0))", "division by zero")
    }

    @Test
    fun shouldNotParseAddIntAndBool() {
        parseAndExpectError("call println(1 + (3 == 2))", "cannot add i64 and bool")
    }

    @Test
    fun shouldNotParseSubIntAndFloat() {
        parseAndExpectError("call println(1 - 1.0)", "cannot subtract i64 and f64")
    }

    @Test
    fun shouldNotParseDivIntAndFloat() {
        parseAndExpectError("call println(1 / 1.0)", "cannot divide i64 and f64")
    }

    @Test
    fun shouldNotParseDivIntAndInt() {
        parseAndExpectError("call println(1 / 1)", "expected floating point subexpressions")
    }

    @Test
    fun shouldNotParseMulIntAndFloat() {
        parseAndExpectError("call println(1 * 1.0)", "cannot multiply i64 and f64")
    }

    @Test
    fun shouldNotParseMulI32AndFloat() {
        parseAndExpectError("call println(i32(1) * 1.0)", "cannot multiply i32 and f64")
    }

    @Test
    fun shouldNotParseModIntAndFloat() {
        parseAndExpectError("call println(1 mod 1.0)", "cannot mod i64 and f64")
    }

    @Test
    fun shouldNotParseBitwiseAndFloat() {
        parseAndExpectError("call println(1.0 & 5)", "expected integer subexpressions")
    }

    @Test
    fun shouldNotParseBitwiseOrFloat() {
        parseAndExpectError("call println(1.0 | 5)", "expected integer subexpressions")
    }

    @Test
    fun shouldNotParseBitwiseXorFloat() {
        parseAndExpectError("call println(1 ^ 5.0)", "expected integer subexpressions")
    }

    @Test
    fun shouldNotParseBitwiseNotFloat() {
        parseAndExpectError("call println(~1.0)", "expected integer subexpression")
    }

    @Test
    fun shouldNotParseBitwiseNotBoolean() {
        parseAndExpectError("call println(~true)", "expected integer subexpression")
    }

    @Test
    fun shouldNotParseEqualIntAndFloat() {
        parseAndExpectError("call println(10 == 7.5)", "cannot compare i64 and f64")
    }

    @Test
    fun shouldNotParseGreaterThanFloatAndInt() {
        parseAndExpectError("call println(10.0 > 7)", "cannot compare f64 and i64")
    }

    @Test
    fun shouldNotParseLessThanBoolAndInt() {
        parseAndExpectError("call println((10.0 == 3.0) < 7)", "cannot compare bool and i64")
    }

    @Test
    fun shouldNotParseLessThanBoolAndBool() {
        parseAndExpectError("call println((10.0 == 3.0) < (7 == 0))", "cannot compare bool and bool")
    }

    @Test
    fun shouldNotParseAddBoolAndBool() {
        parseAndExpectError("call println((10.0 == 3.0) + (7 == 0))", "cannot add bool and bool")
        parseAndExpectError("call println(true + false)", "cannot add bool and bool")
    }

    @Test
    fun shouldNotParseIDivBoolAndBool() {
        parseAndExpectError("call println((10.0 == 3.0) div (7 == 0))", "expected integer subexpressions")
    }

    @Test
    fun shouldNotParseLogicalAndInt() {
        parseAndExpectError("call println(1 and 2)", "expected boolean subexpressions")
    }

    @Test
    fun shouldNotParseLogicalOrFloat() {
        parseAndExpectError("call println(1.0 or 2.0)", "expected boolean subexpressions")
    }

    @Test
    fun shouldNotParseLogicalXorFloat() {
        parseAndExpectError("call println(1.0 xor 2.0)", "expected boolean subexpressions")
    }

    @Test
    fun shouldNotParseLogicalNotFloat() {
        parseAndExpectError("call println(not 1.0)", "expected boolean subexpression")
    }

    @Test
    fun shouldNotParseLogicalXorBoolAndFloat() {
        parseAndExpectError("call println(true xor 2.0)", "cannot xor bool and f64")
    }

    @Test
    fun shouldNotParseNonBooleanIfCondition() {
        parseAndExpectError("call println(if 1 then 0 else 0)", "expected boolean expression, found: 1")
        parseAndExpectError("call println(if 2.0 then 0 else 0)", "expected boolean expression, found: 2.0")
    }

    @Test
    fun shouldNotParseIfWithIncompatibleBranches() {
        parseAndExpectError("call println(if true then 0 else 1.0)", "both branches of an if expression must have the same type")
    }
}
