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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ast.statement.FunCallStatement
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_PRINTLN_I64
import se.dykstrom.jcc.col.ColTests.Companion.FUN_SUM0
import se.dykstrom.jcc.col.ColTests.Companion.FUN_SUM1
import se.dykstrom.jcc.col.ColTests.Companion.FUN_SUM2
import se.dykstrom.jcc.col.ColTests.Companion.verify
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.ast.SubExpression
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.types.*

class ColSemanticsParserFunctionTests : AbstractColSemanticsParserTests() {

    @BeforeEach
    fun setUp() {
        symbolTable.addFunction(FUN_SUM0)
        symbolTable.addFunction(FUN_SUM1)
        symbolTable.addFunction(FUN_SUM2)
    }

    @Test
    fun shouldParsePrintlnFunctionCall0() {
        // Given
        val ident = Identifier(FUN_SUM0.name, Fun.from(FUN_SUM0.argTypes, FUN_SUM0.returnType))
        val functionCall = FunctionCallExpression(0, 0, ident, listOf())
        val statement = funCall(BF_PRINTLN_I64, functionCall)

        // When
        val program = parse("call println(sum())")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnFunctionCall1() {
        // Given
        val ident = Identifier(FUN_SUM1.name, Fun.from(FUN_SUM1.argTypes, FUN_SUM1.returnType))
        val functionCall = FunctionCallExpression(0, 0, ident, listOf(IntegerLiteral.ZERO))
        val statement = funCall(BF_PRINTLN_I64, functionCall)

        // When
        val program = parse("call println(sum(0))")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnFunctionCall2() {
        // Given
        val ident = Identifier(FUN_SUM2.name, Fun.from(FUN_SUM2.argTypes, FUN_SUM2.returnType))
        val subExpression = SubExpression(0, 0, IntegerLiteral.ZERO, IntegerLiteral.ONE)
        val functionCall = FunctionCallExpression(0, 0, ident, listOf(IntegerLiteral.ZERO, subExpression))
        val statement = funCall(BF_PRINTLN_I64, functionCall)

        // When
        val program = parse("call println(sum(0, 0 - 1))")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseStandAloneFunctionCall0() {
        // Given
        val ident = Identifier(FUN_SUM0.name, Fun.from(FUN_SUM0.argTypes, FUN_SUM0.returnType))
        val functionCall = FunctionCallExpression(0, 0, ident, listOf())
        val statement = FunCallStatement(functionCall)

        // When
        val program = parse("call sum()")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldNotParseUnknownFunctionCall() {
        parseAndExpectError("call println(foo())", "undefined function: foo")
    }

    @Test
    fun shouldNotParseCallWithFloatInsteadOfInt() {
        parseAndExpectError("call println(sum(0.3))", "found no match for function call: sum(f64)")
    }

    @Test
    fun shouldNotParseCallWithIntInsteadOfFloat() {
        parseAndExpectError("call println(sqrt(0))", "found no match for function call: sqrt(i64)")
    }
}
