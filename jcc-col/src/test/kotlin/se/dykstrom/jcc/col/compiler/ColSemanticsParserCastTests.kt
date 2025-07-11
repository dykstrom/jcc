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
import se.dykstrom.jcc.col.compiler.ColFunctions.BF_PRINTLN_BOOL
import se.dykstrom.jcc.col.compiler.ColFunctions.BF_PRINTLN_I64
import se.dykstrom.jcc.col.compiler.ColTests.Companion.CAST_1_I32
import se.dykstrom.jcc.col.compiler.ColTests.Companion.verify
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.AndExpression
import se.dykstrom.jcc.common.ast.CastToI64Expression
import se.dykstrom.jcc.common.ast.GreaterExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO

class ColSemanticsParserCastTests : AbstractColSemanticsParserTests() {

    @Test
    fun shouldMakeImplicitCastExplicit() {
        // I32 -> I64
        verify(parse("call println(i32(1) + 0)"), funCall(BF_PRINTLN_I64, AddExpression(CastToI64Expression(CAST_1_I32), ZERO)))
        verify(parse("call println(i32(1) & 0)"), funCall(BF_PRINTLN_I64, AndExpression(CastToI64Expression(CAST_1_I32), ZERO)))
        verify(parse("call println(i32(1) > 0)"), funCall(BF_PRINTLN_BOOL, GreaterExpression(CastToI64Expression(CAST_1_I32), ZERO)))
    }

    @Test
    fun shouldNotReplaceCastWithInvalidArgs() {
        parseAndExpectError("call println(f64(0.0))", "found no match for function call: f64(f64)")
        parseAndExpectError("call println(i64(0))", "found no match for function call: i64(i64)")
    }
}
