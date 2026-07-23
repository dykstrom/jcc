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
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.col.ColTests.Companion.NT_I64
import se.dykstrom.jcc.col.ColTests.Companion.verify
import se.dykstrom.jcc.col.ast.expression.BecomeExpression
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.Declaration
import se.dykstrom.jcc.common.ast.Expression
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.ast.IfExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import se.dykstrom.jcc.common.ast.LessOrEqualExpression
import se.dykstrom.jcc.common.error.SyntaxException
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.Identifier

/**
 * Become is parsed liberally as a factor, so it parses anywhere a factor can appear; whether a
 * given position is actually a tail call is verified later, in semantic analysis. The only thing
 * the grammar enforces is that become is followed by a function call.
 */
class ColSyntaxParserBecomeTests : AbstractColSyntaxParserTests() {

    // A reference to argument n, whose type is not yet known after parsing
    private val ideN = IdentifierDerefExpression(0, 0, Identifier("n", null))
    // A call to f, whose argument and return types are not yet known after parsing
    private val callF = FunctionCallExpression(0, 0, Identifier("f", Fun.from(listOf(null), null)), listOf(ideN))

    @Test
    fun shouldParseBecomeAsFunctionBody() {
        // Given
        val statement = funF(BecomeExpression(callF))

        // When
        val program = parse("fun f(n as i64) -> i64 := become f(n)")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseBecomeInElseBranch() {
        // Given
        val ifExpression = IfExpression(LessOrEqualExpression(ideN, ZERO), ZERO, BecomeExpression(callF))
        val statement = funF(ifExpression)

        // When
        val program = parse("fun f(n as i64) -> i64 := if n <= 0 then 0 else become f(n)")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseBecomeAsOperand() {
        // Parses fine even though it is not in tail position; semantics rejects it later

        // Given
        val statement = funF(AddExpression(0, 0, ideN, BecomeExpression(callF)))

        // When
        val program = parse("fun f(n as i64) -> i64 := n + become f(n)")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldNotParseBecomeWithoutFunctionCall() {
        assertThrows<SyntaxException> { parse("fun f(n as i64) -> i64 := become 5") }
        assertThrows<SyntaxException> { parse("fun f(n as i64) -> i64 := become n") }
    }

    /**
     * Wraps the given [body] expression in a function definition for {@code f(n as i64) -> i64}.
     */
    private fun funF(body: Expression): FunctionDefinitionStatement {
        val identifier = Identifier("f", Fun.from(listOf(NT_I64), NT_I64))
        val declarations = listOf(Declaration(0, 0, "n", NT_I64))
        return FunctionDefinitionStatement(0, 0, identifier, declarations, body)
    }
}
