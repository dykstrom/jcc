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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ColTests.Companion.IL_17
import se.dykstrom.jcc.col.ColTests.Companion.NT_I64
import se.dykstrom.jcc.col.ColTests.Companion.verify
import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.DeclarationAssignment
import se.dykstrom.jcc.common.ast.FloatLiteral
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.Identifier

class ColSyntaxParserValTests : AbstractColSyntaxParserTests() {

    @Test
    fun shouldParseTypedVal() {
        val expected = ValDeclarationStatement(DeclarationAssignment("limit", NT_I64, IntegerLiteral(0, 0, 10_000)))
        verify(parse("val limit as i64 := 10_000"), expected)
    }

    @Test
    fun shouldParseUntypedVal() {
        val expected = ValDeclarationStatement(DeclarationAssignment("phi", null, FloatLiteral(0, 0, "1.618")))
        verify(parse("val phi := 1.618"), expected)
    }

    @Test
    fun shouldParseValWithFunctionCallInitializer() {
        val millisCall = FunctionCallExpression(0, 0, Identifier("millis", Fun.from(listOf(), null)), listOf())
        val expected = ValDeclarationStatement(DeclarationAssignment("start", null, millisCall))
        verify(parse("val start := millis()"), expected)
    }

    @Test
    fun shouldParseFunctionTypedVal() {
        val funType = Fun.from(listOf(NT_I64), NT_I64)
        val reference = IdentifierDerefExpression(0, 0, Identifier("inc", null))
        val expected = ValDeclarationStatement(DeclarationAssignment("inc2", funType, reference))
        verify(parse("val inc2 as (i64) -> i64 := inc"), expected)
    }

    @Test
    fun shouldParseValWithoutInitializer() {
        // Missing initializer is a semantic error, not a parse error
        val expected = ValDeclarationStatement(DeclarationAssignment("x", NT_I64, null))
        verify(parse("val x as i64"), expected)
    }

    @Test
    fun shouldParseValWithoutTypeAndInitializer() {
        val expected = ValDeclarationStatement(DeclarationAssignment("x", null, null))
        verify(parse("val x"), expected)
    }

    @Test
    fun shouldParseValWithExpressionInitializer() {
        val expression = AddExpression(0, 0, IL_17, IntegerLiteral(0, 0, 1))
        val expected = ValDeclarationStatement(DeclarationAssignment("x", null, expression))
        verify(parse("val x := 17 + 1"), expected)
    }

    @Test
    fun shouldParseValBoundWithEquals() {
        // Binding with '=' parses; it is rejected as a semantic error (see ColSemanticsParserValTests)
        val walrus = parse("val x := 5").statements[0] as ValDeclarationStatement
        assertFalse(walrus.usesEquals())
        val equals = parse("val x = 5").statements[0] as ValDeclarationStatement
        assertTrue(equals.usesEquals())
    }
}
