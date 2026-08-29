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

class ColSemanticsParserCastTests : AbstractColSemanticsParserTests() {

    @Test
    fun shouldMakeImplicitCastExplicit() {
        // I32 -> I64
        verify(parse("call println(i32(1) + 0)"), funCall(BF_PRINTLN_I64, AddExpression(CastToI64Expression(CAST_1_I32), ZERO)))
        verify(parse("call println(i32(1) & 0)"), funCall(BF_PRINTLN_I64, AndExpression(CastToI64Expression(CAST_1_I32), ZERO)))
        verify(parse("call println(i32(1) > 0)"), funCall(BF_PRINTLN_BOOL, GreaterExpression(CastToI64Expression(CAST_1_I32), ZERO)))
        
        // F32 -> F64
        verify(parse("call println(f32(1.0) + 1.0)"), funCall(BF_PRINTLN_F64, AddExpression(CastToF64Expression(CAST_1_0_F32), FL_1_0)))
        verify(parse("call println(f32(1.0) > 1.0)"), funCall(BF_PRINTLN_BOOL, GreaterExpression(CastToF64Expression(CAST_1_0_F32), FL_1_0)))
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
    fun shouldNotCastToUnrelatedType() {
        parseAndExpectError("""call println(i64("17"))""", "found no match for function call: i64(string)")
        parseAndExpectError("call println(f64(true))", "found no match for function call: f64(bool)")
    }
}
