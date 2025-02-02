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
import se.dykstrom.jcc.col.ast.statement.PrintlnStatement
import se.dykstrom.jcc.col.compiler.ColTests.Companion.CAST_1_I32
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FL_1_0
import se.dykstrom.jcc.col.compiler.ColTests.Companion.verify
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO

class ColSemanticsParserCastTests : AbstractColSemanticsParserTests() {

    @Test
    fun shouldTurnCastIntoExpression() {
        verify(parse("println f64(0)"), PrintlnStatement(CastToF64Expression(ZERO)))
        verify(parse("println i32(0)"), PrintlnStatement(CastToI32Expression(ZERO)))
        verify(parse("println i64(1.0)"), PrintlnStatement(CastToI64Expression(FL_1_0)))
    }

    @Test
    fun shouldMakeImplicitCastExplicit() {
        // I32 -> I64
        verify(parse("println i32(1) + 0"), PrintlnStatement(AddExpression(CastToI64Expression(CAST_1_I32), ZERO)))
        verify(parse("println i32(1) & 0"), PrintlnStatement(AndExpression(CastToI64Expression(CAST_1_I32), ZERO)))
        verify(parse("println i32(1) > 0"), PrintlnStatement(GreaterExpression(CastToI64Expression(CAST_1_I32), ZERO)))
        verify(parse("println f64(i32(0))"), PrintlnStatement(CastToF64Expression(CastToI64Expression(CastToI32Expression(ZERO)))))
    }

    @Test
    fun shouldNotReplaceCastWithInvalidArgs() {
        parseAndExpectError("println f64(0.0)", "found no match for function call: f64(f64)")
        parseAndExpectError("println i32(0.0)", "found no match for function call: i32(f64)")
        parseAndExpectError("println i64(0)", "found no match for function call: i64(i64)")
    }
}
