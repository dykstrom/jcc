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
import se.dykstrom.jcc.col.compiler.ColTests.Companion.verify
import se.dykstrom.jcc.col.types.NamedType
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.Declaration
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.types.Type

class ColSyntaxParserUserFunctionTests : AbstractColSyntaxParserTests() {

    @Test
    fun shouldParseExpressionFunction() {
        // Given
        val argTypes = listOf<Type>()
        val returnType = NamedType("i64")
        val identifier = Identifier("foo", Fun.from(argTypes, returnType))
        val declarations = listOf<Declaration>()
        val expression = ZERO
        val statement = FunctionDefinitionStatement(0, 0, identifier, declarations, expression)

        // When
        val program = parse("fun foo() -> i64 = 0")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseExpressionFunctionWithOneArg() {
        // Given
        val argTypes = listOf(NamedType("f64"))
        val returnType = NamedType("i64")
        val identifier = Identifier("foo", Fun.from(argTypes, returnType))
        val declarations = listOf(Declaration(0, 0, "a", NamedType("f64")))
        val expression = ZERO
        val statement = FunctionDefinitionStatement(0, 0, identifier, declarations, expression)

        // When
        val program = parse("fun foo(a as f64) -> i64 = 0")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseExpressionFunctionWithTwoArgs() {
        // Given
        val argTypes = listOf(NamedType("f64"), NamedType("i64"))
        val returnType = NamedType("f64")
        val definedIdentifier = Identifier("foo_2", Fun.from(argTypes, returnType))
        val declarations = listOf(
            Declaration(0, 0, "a", NamedType("f64")),
            Declaration(0, 0, "b", NamedType("i64"))
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
    fun shouldParseExpressionFunctionThatUsesArgs() {
        // Given
        val argTypes = listOf(NamedType("i64"), NamedType("i64"))
        val returnType = NamedType("i64")
        val identifier = Identifier("foo", Fun.from(argTypes, returnType))
        val declarations = listOf(
            Declaration(0, 0, "a", NamedType("i64")),
            Declaration(0, 0, "b", NamedType("i64"))
        )
        val expression = AddExpression(0, 0, IDE_UNK_A, IDE_UNK_B)
        val statement = FunctionDefinitionStatement(0, 0, identifier, declarations, expression)

        // When
        val program = parse("fun foo(a as i64, b as i64) -> i64 = a + b")

        // Then
        verify(program, statement)
    }
}
