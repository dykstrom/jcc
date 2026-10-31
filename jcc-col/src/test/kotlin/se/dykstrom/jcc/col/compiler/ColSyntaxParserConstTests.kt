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
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.col.ColTests.Companion.IL_17
import se.dykstrom.jcc.col.ColTests.Companion.NT_I64
import se.dykstrom.jcc.col.ColTests.Companion.verify
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.ConstDeclarationStatement
import se.dykstrom.jcc.common.ast.DeclarationAssignment
import se.dykstrom.jcc.common.ast.Expression
import se.dykstrom.jcc.common.ast.FloatLiteral
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.error.SyntaxException
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.types.NamedType
import se.dykstrom.jcc.common.types.Type

class ColSyntaxParserConstTests : AbstractColSyntaxParserTests() {

    @Test
    fun shouldParseTypedConst() {
        val expected = constStatement("LIMIT", NT_I64, IntegerLiteral(0, 0, 10_000))
        verify(parse("const LIMIT as i64 := 10_000"), expected)
    }

    @Test
    fun shouldParseUntypedConst() {
        val expected = constStatement("PHI", null, FloatLiteral(0, 0, "1.618"))
        verify(parse("const PHI := 1.618"), expected)
    }

    @Test
    fun shouldParseConstWithExpressionInitializer() {
        val expected = constStatement("X", null, AddExpression(0, 0, IL_17, IntegerLiteral(0, 0, 1)))
        verify(parse("const X := 17 + 1"), expected)
    }

    @Test
    fun shouldParseConstWithConversionInitializer() {
        val meters = Identifier("Meters", Fun.from(listOf(null), null))
        val call = FunctionCallExpression(0, 0, meters, listOf(FloatLiteral(0, 0, "7.3")))
        val expected = constStatement("LENGTH", NamedType("Meters"), call)
        verify(parse("const LENGTH as Meters := Meters(7.3)"), expected)
    }

    @Test
    fun shouldParseConstWithoutInitializer() {
        // Missing initializer is a semantic error, not a parse error
        verify(parse("const X as i64"), constStatement("X", NT_I64, null))
    }

    @Test
    fun shouldParseConstBoundWithEquals() {
        // Binding with '=' parses; it is rejected as a semantic error (see ColSemanticsParserConstTests)
        val walrus = parse("const X := 5").statements[0] as ConstDeclarationStatement
        assertFalse(walrus.usesEquals())
        val equals = parse("const X = 5").statements[0] as ConstDeclarationStatement
        assertTrue(equals.usesEquals())
    }

    @Test
    fun shouldNotParseConstWithoutName() {
        assertThrows<SyntaxException> { parse("const := 5") }
    }

    private fun constStatement(name: String, type: Type?, expression: Expression?) =
        ConstDeclarationStatement(listOf(DeclarationAssignment(name, type, expression)))
}
