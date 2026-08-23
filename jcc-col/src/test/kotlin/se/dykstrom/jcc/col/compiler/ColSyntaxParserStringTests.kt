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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.col.ColTests.Companion.verify
import se.dykstrom.jcc.col.ast.expression.MalformedStringLiteral
import se.dykstrom.jcc.common.ast.StringLiteral
import se.dykstrom.jcc.common.error.SyntaxException

/**
 * Parsing of string literals and their escape sequences. A literal that cannot be decoded becomes
 * a [MalformedStringLiteral] marker here; the error itself is reported in semantic analysis, see
 * [ColSemanticsParserStringTests].
 */
class ColSyntaxParserStringTests : AbstractColSyntaxParserTests() {

    @Test
    fun shouldParseStringLiteral() {
        verify(parse("""call println("hello")"""), printlnCall(StringLiteral(0, 0, "hello")))
    }

    @Test
    fun shouldParseEmptyStringLiteral() {
        verify(parse("""call println("")"""), printlnCall(StringLiteral(0, 0, "")))
    }

    @Test
    fun shouldNotLexOperatorsInsideStringLiteral() {
        verify(parse("""call println("1 + 2 // not a comment")"""), printlnCall(StringLiteral(0, 0, "1 + 2 // not a comment")))
    }

    @Test
    fun shouldParseCStyleEscapes() {
        assertEquals("\n", decode("""\n"""))
        assertEquals("\t", decode("""\t"""))
        assertEquals("\r", decode("""\r"""))
        assertEquals("\\", decode("""\\"""))
        assertEquals("\"", decode("""\""""))
    }

    @Test
    fun shouldParseEscapesMixedWithText() {
        assertEquals("a\tb\nc", decode("""a\tb\nc"""))
    }

    @Test
    fun shouldParseCodePointEscape() {
        assertEquals("é", decode("""\u{e9}"""))
        assertEquals("é", decode("""\u{E9}"""))
        // Outside the basic multilingual plane: a surrogate pair in Java's UTF-16 String
        assertEquals("😀", decode("""\u{1F600}"""))
        assertEquals("café", decode("""caf\u{e9}"""))
    }

    @Test
    fun shouldKeepNonAsciiSourceTextIntact() {
        // The source is read as UTF-8 (Antlr4Utils.asInputStream), so these arrive unchanged
        assertEquals("café 😀", decode("café 😀"))
    }

    @Test
    fun shouldParseStringLiteralAsMarkerOnUnknownEscape() {
        assertTrue(parseExpression("""call println("a\qb")""") is MalformedStringLiteral)
    }

    @Test
    fun shouldParseStringLiteralAsMarkerOnMalformedCodePointEscape() {
        assertTrue(parseExpression("""call println("\uABC")""") is MalformedStringLiteral)
        assertTrue(parseExpression("""call println("\u{}")""") is MalformedStringLiteral)
        assertTrue(parseExpression("""call println("\u{zz}")""") is MalformedStringLiteral)
        assertTrue(parseExpression("""call println("\u{1F600")""") is MalformedStringLiteral)
    }

    @Test
    fun shouldParseStringLiteralAsMarkerOnInvalidCodePoint() {
        assertTrue(parseExpression("""call println("\u{110000}")""") is MalformedStringLiteral)
        assertTrue(parseExpression("""call println("\u{D800}")""") is MalformedStringLiteral)
    }

    @Test
    fun shouldParseStringLiteralAsMarkerOnNul() {
        assertTrue(parseExpression("""call println("\u{0}")""") is MalformedStringLiteral)
    }

    @Test
    fun shouldNotParseUnterminatedStringLiteral() {
        // No token can match, so this one is a lexer error rather than a marker node
        assertThrows<SyntaxException> { parse("""call println("hello)""") }
    }

    @Test
    fun shouldNotParseStringLiteralSpanningLines() {
        assertThrows<SyntaxException> { parse("call println(\"hello\nworld\")") }
    }

    /**
     * Returns the decoded value of a string literal written with the given body, quotes excluded.
     */
    private fun decode(body: String): String {
        val expression = parseExpression("""call println("$body")""")
        return (expression as StringLiteral).value
    }
}
