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
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FL_1_0
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IDE_UNK_A
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IDE_UNK_B
import se.dykstrom.jcc.col.compiler.ColTests.Companion.NT_F64
import se.dykstrom.jcc.col.compiler.ColTests.Companion.NT_I64
import se.dykstrom.jcc.col.compiler.ColTests.Companion.verify
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.Declaration
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.Identifier

class ColSyntaxParserUserFunctionTests : AbstractColSyntaxParserTests() {

    @Test
    fun shouldParseFunction() {
        // Given
        val identifier = Identifier("foo", Fun.from(listOf(), NT_I64))
        val declarations = listOf<Declaration>()
        val statement = FunctionDefinitionStatement(0, 0, identifier, declarations, ZERO)

        // When
        val program = parse("fun foo() -> i64 = 0")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseFunctionWithOneArg() {
        // Given
        val identifier = Identifier("foo", Fun.from(listOf(NT_F64), NT_I64))
        val declarations = listOf(Declaration(0, 0, "a", NT_F64))
        val statement = FunctionDefinitionStatement(0, 0, identifier, declarations, ZERO)

        // When
        val program = parse("fun foo(a as f64) -> i64 = 0")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseFunctionWithTwoArgs() {
        // Given
        val argTypes = listOf(NT_F64, NT_I64)
        val definedIdentifier = Identifier("foo_2", Fun.from(argTypes, NT_F64))
        val declarations = listOf(
            Declaration(0, 0, "a", NT_F64),
            Declaration(0, 0, "b", NT_I64)
        )
        // We do not yet know the argument and return types of the called function
        val calledIdentifier = Identifier("bar_1", Fun.from(listOf(null), null))
        val expression = FunctionCallExpression(0, 0, calledIdentifier, listOf(FL_1_0))
        val statement = FunctionDefinitionStatement(0, 0, definedIdentifier, declarations, expression)

        // When
        val program = parse("fun foo_2(a as f64, b as i64) -> f64 = bar_1(1.0)")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseFunctionThatUsesArgs() {
        // Given
        val argTypes = listOf(NT_I64, NT_I64)
        val identifier = Identifier("foo", Fun.from(argTypes, NT_I64))
        val declarations = listOf(
            Declaration(0, 0, "a", NT_I64),
            Declaration(0, 0, "b", NT_I64)
        )
        val expression = AddExpression(0, 0, IDE_UNK_A, IDE_UNK_B)
        val statement = FunctionDefinitionStatement(0, 0, identifier, declarations, expression)

        // When
        val program = parse("fun foo(a as i64, b as i64) -> i64 = a + b")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseFunctionWithFunctionTypeArg() {
        // Given
        val argType = Fun.from(listOf(NT_F64), NT_F64)
        val identifier = Identifier("foo", Fun.from(listOf(argType), NT_I64))
        val declarations = listOf(Declaration(0, 0, "a", argType))
        val statement = FunctionDefinitionStatement(0, 0, identifier, declarations, ZERO)

        // When
        val program = parse("fun foo(a as (f64) -> f64) -> i64 = 0")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseFunctionWithFunctionTypeReturn() {
        // Given
        val returnType = Fun.from(listOf(NT_F64), NT_F64)
        val identifier = Identifier("foo", Fun.from(listOf(NT_F64), returnType))
        val declarations = listOf(Declaration(0, 0, "a", NT_F64))
        val statement = FunctionDefinitionStatement(0, 0, identifier, declarations, ZERO)

        // When
        val program = parse("fun foo(a as f64) -> (f64) -> f64 = 0")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseFunctionThatUsesFunctionTypeArg() {
        // Given
        val funI64ToI64 = Fun.from(listOf(NT_I64), NT_I64)
        val identFoo = Identifier("foo", Fun.from(listOf(funI64ToI64), NT_I64))
        val declarations = listOf(Declaration(0, 0, "bar", funI64ToI64))
        // We don't yet know the details of function bar
        val identBar = Identifier("bar", Fun.from(listOf(null), null))
        val fce = FunctionCallExpression(0, 0, identBar, listOf(ZERO))
        val statement = FunctionDefinitionStatement(0, 0, identFoo, declarations, fce)

        // When
        val program = parse("fun foo(bar as (i64) -> i64) -> i64 = bar(0)")

        // Then
        verify(program, statement)
    }
}
