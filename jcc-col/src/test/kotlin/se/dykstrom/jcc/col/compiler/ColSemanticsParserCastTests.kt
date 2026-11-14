/*
 * Copyright (C) 2024 Johan Dykstrom
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

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.compiler.ColSymbols.*
import se.dykstrom.jcc.col.ColTests.Companion.CAST_1_0_F32
import se.dykstrom.jcc.col.ColTests.Companion.CAST_1_I32
import se.dykstrom.jcc.col.ColTests.Companion.FL_1_0
import se.dykstrom.jcc.col.ColTests.Companion.verify
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.I128
import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement
import org.junit.jupiter.api.Assertions.assertEquals

class ColSemanticsParserCastTests : AbstractColSemanticsParserTests() {

    @Test
    fun shouldMakeImplicitCastExplicit() {
        // I32 -> I64
        verify(parse("call println(i32(1) + 0)"), funCall(BF_PRINTLN_I64, AddExpression(CastToIntExpression(CAST_1_I32, I64.INSTANCE), ZERO)))
        verify(parse("call println(i32(1) & 0)"), funCall(BF_PRINTLN_I64, AndExpression(CastToIntExpression(CAST_1_I32, I64.INSTANCE), ZERO)))
        verify(parse("call println(i32(1) > 0)"), funCall(BF_PRINTLN_BOOL, GreaterExpression(CastToIntExpression(CAST_1_I32, I64.INSTANCE), ZERO)))

        // I32, I64 -> I128
        verify(parse("call println(i32(1) > 1i128)"), funCall(BF_PRINTLN_BOOL, GreaterExpression(CastToIntExpression(CAST_1_I32, I128.INSTANCE), ONE_I128)))
        verify(parse("call println(1i128 > 0)"), funCall(BF_PRINTLN_BOOL, GreaterExpression(ONE_I128, CastToIntExpression(ZERO, I128.INSTANCE))))
        
        // F32 -> F64
        verify(parse("call println(f32(1.0) + 1.0)"), funCall(BF_PRINTLN_F64, AddExpression(CastToFloatExpression(CAST_1_0_F32, F64.INSTANCE), FL_1_0)))
        verify(parse("call println(f32(1.0) > 1.0)"), funCall(BF_PRINTLN_BOOL, GreaterExpression(CastToFloatExpression(CAST_1_0_F32, F64.INSTANCE), FL_1_0)))
    }

    @Test
    fun shouldResolveSameTypeCastToIdentity() {
        // A same-type cast is legal and is the identity: it survives semantic analysis as an
        // ordinary call to the same-type overload, and ColFunctions inlines it away to the
        // argument itself. Writing one is how a programmer pins a type without knowing whether a
        // conversion is actually needed, so rejecting it would be gratuitous.
        verify(parse("call println(f64(1.0))"), funCall(BF_PRINTLN_F64, funCallExpr(BF_F64_F64, FL_1_0)))
        verify(parse("call println(i64(0))"), funCall(BF_PRINTLN_I64, funCallExpr(BF_I64_I64, ZERO)))
        verify(parse("call println(f32(f32(1.0)))"), funCall(BF_PRINTLN_F32, funCallExpr(BF_F32_F32, CAST_1_0_F32)))
        verify(parse("call println(i32(i32(1)))"), funCall(BF_PRINTLN_I32, funCallExpr(BF_I32_I32, CAST_1_I32)))
    }

    @Test
    fun shouldResolveCastBetweenEachPairOfNumericTypes() {
        val sources = listOf("i32(1)" to CAST_1_I32, "0" to ZERO, "1i128" to ONE_I128, "f32(1.0)" to CAST_1_0_F32, "1.0" to FL_1_0)
        val casts = mapOf(
            "i32" to listOf(BF_I32_I32, BF_I32_I64, BF_I32_I128, BF_I32_F32, BF_I32_F64),
            "i64" to listOf(BF_I64_I32, BF_I64_I64, BF_I64_I128, BF_I64_F32, BF_I64_F64),
            "i128" to listOf(BF_I128_I32, BF_I128_I64, BF_I128_I128, BF_I128_F32, BF_I128_F64),
            "f32" to listOf(BF_F32_I32, BF_F32_I64, BF_F32_I128, BF_F32_F32, BF_F32_F64),
            "f64" to listOf(BF_F64_I32, BF_F64_I64, BF_F64_I128, BF_F64_F32, BF_F64_F64),
        )
        casts.forEach { (target, functions) ->
            sources.zip(functions).forEach { (source, function) ->
                // The cast initializes a val, because println does not take every type
                val statement = parse("val x := $target(${source.first})").statements[0] as ValDeclarationStatement
                assertEquals(function.returnType, statement.declaration().type())
                assertEquals(funCallExpr(function, source.second), statement.declaration().expression())
            }
        }
    }

    @Test
    fun shouldNotCastToUnrelatedType() {
        parseAndExpectError("""call println(i64("17"))""", "found no match for function call: i64(string)")
        parseAndExpectError("call println(f64(true))", "found no match for function call: f64(bool)")
    }

    companion object {
        private val ONE_I128 = IntegerLiteral(0, 0, "1", I128.INSTANCE)
    }
}
