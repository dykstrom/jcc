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

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ast.statement.FunCallStatement
import se.dykstrom.jcc.col.ColTests.Companion.IL_5
import se.dykstrom.jcc.col.ColTests.Companion.NT_F64
import se.dykstrom.jcc.col.ColTests.Companion.NT_I64
import se.dykstrom.jcc.col.ColTests.Companion.NT_VOID
import se.dykstrom.jcc.col.ColTests.Companion.verify
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral.ONE
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import se.dykstrom.jcc.common.ast.SubExpression
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.Identifier

class ColSyntaxParserFunctionTests : AbstractColSyntaxParserTests() {

    @Test
    fun shouldParsePrintlnFunctionCall0() {
        // Given
        val ident = Identifier("foo", Fun.from(listOf(), null))
        val functionCall = FunctionCallExpression(ident, listOf())
        val statement = printlnCall(functionCall)

        // When
        val program = parse("call println(foo())")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnFunctionCall1() {
        // Given
        val ident = Identifier("foo", Fun.from(listOf(null), null))
        val functionCall = FunctionCallExpression(ident, listOf(IL_5))
        val statement = printlnCall(functionCall)

        // When
        val program = parse("call println(foo(5))")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnFunctionCall2() {
        // Given
        val ident = Identifier("foo", Fun.from(listOf(null, null), null))
        val subExpression = SubExpression(ZERO, ONE)
        val functionCall = FunctionCallExpression(ident, listOf(IL_5, subExpression))
        val statement = printlnCall(functionCall)

        // When
        val program = parse("call println(foo(5, 0 - 1))")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseStandAloneFunctionCall() {
        // Given
        val ident = Identifier("foo", Fun.from(listOf(), null))
        val functionCall = FunctionCallExpression(ident, listOf())
        val statement = FunCallStatement(functionCall)

        // When
        val program = parse("call foo()")

        // Then
        verify(program, statement)
    }
}
