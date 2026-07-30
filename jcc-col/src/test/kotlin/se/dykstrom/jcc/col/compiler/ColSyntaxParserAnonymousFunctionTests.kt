/*
 * Copyright (C) 2026 Johan Dykstrom
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
import se.dykstrom.jcc.col.ColTests.Companion.IDE_UNK_A
import se.dykstrom.jcc.col.ColTests.Companion.IDE_UNK_B
import se.dykstrom.jcc.col.ColTests.Companion.IDE_UNK_X
import se.dykstrom.jcc.col.ColTests.Companion.IL_5
import se.dykstrom.jcc.col.ColTests.Companion.NT_I64
import se.dykstrom.jcc.col.ColTests.Companion.NT_VOID
import se.dykstrom.jcc.col.ColTests.Companion.verify
import se.dykstrom.jcc.col.ast.expression.AnonymousFunctionExpression
import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.Declaration
import se.dykstrom.jcc.common.ast.DeclarationAssignment
import se.dykstrom.jcc.common.ast.Expression
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement
import se.dykstrom.jcc.common.ast.IntegerLiteral.ONE
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.Identifier

class ColSyntaxParserAnonymousFunctionTests : AbstractColSyntaxParserTests() {

    @Test
    fun shouldParseAnonymousFunctionWithNoArgs() {
        // Given
        val lambda = AnonymousFunctionExpression(listOf(), IL_5, NT_I64)
        val statement = valStatement("f", lambda)

        // When
        val program = parse("val f := fun() -> i64 := 5")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseAnonymousFunctionWithTwoArgs() {
        // Given
        val declarations = listOf(
            Declaration(0, 0, "a", NT_I64),
            Declaration(0, 0, "b", NT_I64)
        )
        val lambda = AnonymousFunctionExpression(declarations, AddExpression(0, 0, IDE_UNK_A, IDE_UNK_B), NT_I64)
        val statement = valStatement("add", lambda)

        // When
        val program = parse("val add := fun(a as i64, b as i64) -> i64 := a + b")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseAnonymousFunctionWithInferredReturnType() {
        // Given
        val declarations = listOf(Declaration(0, 0, "a", NT_I64))
        val lambda = AnonymousFunctionExpression(declarations, AddExpression(0, 0, IDE_UNK_A, ONE), null)
        val statement = valStatement("inc", lambda)

        // When
        val program = parse("val inc := fun(a as i64) := a + 1")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseAnonymousFunctionWithMissingParameterType() {
        // Given
        // An omitted parameter type parses, and is rejected in semantic analysis
        val declarations = listOf(Declaration(0, 0, "a", NT_VOID))
        val lambda = AnonymousFunctionExpression(declarations, IDE_UNK_A, NT_I64)
        val statement = valStatement("f", lambda)

        // When
        val program = parse("val f := fun(a) -> i64 := a")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseAnonymousFunctionAsArgument() {
        // Given
        // The comma terminates the greedy body, so this is a call with two arguments
        val declarations = listOf(Declaration(0, 0, "a", NT_I64))
        val lambda = AnonymousFunctionExpression(declarations, AddExpression(0, 0, IDE_UNK_A, ONE), NT_I64)
        val identifier = Identifier("apply", Fun.from(listOf(null, null), null))
        val expression = FunctionCallExpression(0, 0, identifier, listOf(lambda, IL_5))
        val statement = valStatement("r", expression)

        // When
        val program = parse("val r := apply(fun(a as i64) -> i64 := a + 1, 5)")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseAnonymousFunctionReturnedFromFunction() {
        // Given
        val lambdaDeclarations = listOf(Declaration(0, 0, "x", NT_I64))
        val lambda = AnonymousFunctionExpression(lambdaDeclarations, AddExpression(0, 0, IDE_UNK_X, ONE), NT_I64)
        val returnType = Fun.from(listOf(NT_I64), NT_I64)
        val identifier = Identifier("adder", Fun.from(listOf(NT_I64), returnType))
        val declarations = listOf(Declaration(0, 0, "n", NT_I64))
        val statement = FunctionDefinitionStatement(0, 0, identifier, declarations, lambda)

        // When
        val program = parse("fun adder(n as i64) -> (i64) -> i64 := fun(x as i64) -> i64 := x + 1")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseNestedAnonymousFunctions() {
        // Given
        val inner = AnonymousFunctionExpression(listOf(Declaration(0, 0, "b", NT_I64)), IDE_UNK_B, NT_I64)
        val outer = AnonymousFunctionExpression(listOf(Declaration(0, 0, "a", NT_I64)), inner, null)
        val statement = valStatement("f", outer)

        // When
        val program = parse("val f := fun(a as i64) := fun(b as i64) -> i64 := b")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseParenthesizedAnonymousFunctionAsOperand() {
        // Given
        // The greedy body ends at the closing parenthesis, so 'println' receives the lambda,
        // and the result of the call is added to 5
        val lambda = AnonymousFunctionExpression(listOf(), ONE, NT_I64)
        val println = Identifier("println", Fun.from(listOf(null), null))
        val expression = AddExpression(0, 0, FunctionCallExpression(0, 0, println, listOf(lambda)), IL_5)
        val statement = valStatement("r", expression)

        // When
        val program = parse("val r := println((fun() -> i64 := 1)) + 5")

        // Then
        verify(program, statement)
    }

    private fun valStatement(name: String, expression: Expression) =
        ValDeclarationStatement(0, 0, DeclarationAssignment(0, 0, name, null, expression), false)
}
