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
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ast.statement.ValDeclarationStatement
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement
import se.dykstrom.jcc.common.types.Bool
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Str

/**
 * Semantic analysis of the string type: where `string` may appear, which operators accept it, and
 * the conversions that stay forbidden. Code generation is out of scope here, so nothing in these
 * tests prints a string.
 */
class ColSemanticsParserStringTests : AbstractColSemanticsParserTests() {

    @Test
    fun shouldParseTypedStringVal() {
        val program = parse(
            """
            val s as string := "hello"
            fun use(x as string) -> i64 := 0
            call use(s)
            """.trimIndent()
        )
        val statement = program.statements[0] as ValDeclarationStatement
        assertEquals(Str.INSTANCE, statement.declaration().type())
    }

    @Test
    fun shouldInferStringTypeFromLiteral() {
        val program = parse(
            """
            val s := "hello"
            fun use(x as string) -> i64 := 0
            call use(s)
            """.trimIndent()
        )
        val statement = program.statements[0] as ValDeclarationStatement
        assertEquals(Str.INSTANCE, statement.declaration().type())
    }

    @Test
    fun shouldParseStringParameterAndReturnType() {
        val program = parse("""fun echo(s as string) -> string := s""")
        val statement = program.statements[0] as FunctionDefinitionStatement
        assertEquals(Str.INSTANCE, statement.declarations()[0].type())
        assertEquals(Fun.from(listOf(Str.INSTANCE), Str.INSTANCE), statement.identifier().type())
    }

    @Test
    fun shouldParseStringFunctionType() {
        val program = parse(
            """
            fun size(s as string) -> i64 := 0
            val f as (string) -> i64 := size
            fun use(g as (string) -> i64) -> i64 := 0
            call use(f)
            """.trimIndent()
        )
        val statement = program.statements[1] as ValDeclarationStatement
        assertEquals(Fun.from(listOf(Str.INSTANCE), I64.INSTANCE), statement.declaration().type())
    }

    @Test
    fun shouldParseStringAlias() {
        val program = parse(
            """
            alias Text as string
            val s as Text := "hello"
            fun use(x as string) -> i64 := 0
            call use(s)
            """.trimIndent()
        )
        val statement = program.statements[1] as ValDeclarationStatement
        assertEquals(Str.INSTANCE, statement.declaration().type())
    }

    @Test
    fun shouldConcatenateStrings() {
        val program = parse("""fun greet(name as string) -> string := "hello, " + name""")
        val statement = program.statements[0] as FunctionDefinitionStatement
        assertEquals(Str.INSTANCE, typeManager.getType(statement.expression()))
    }

    @Test
    fun shouldCompareStringsForEquality() {
        val program = parse("""fun same(a as string, b as string) -> bool := a == b""")
        val statement = program.statements[0] as FunctionDefinitionStatement
        assertEquals(Bool.INSTANCE, typeManager.getType(statement.expression()))
    }

    @Test
    fun shouldCompareStringsForInequality() {
        val program = parse("""fun differ(a as string, b as string) -> bool := a != b""")
        val statement = program.statements[0] as FunctionDefinitionStatement
        assertEquals(Bool.INSTANCE, typeManager.getType(statement.expression()))
    }

    @Test
    fun shouldNotAddStringAndNumber() {
        parseAndExpectError("""fun f(s as string) -> string := s + 1""", "cannot add string and i64")
    }

    @Test
    fun shouldNotAddNumberAndString() {
        parseAndExpectError("""fun f(s as string) -> string := 1 + s""", "cannot add i64 and string")
    }

    @Test
    fun shouldNotSubtractStrings() {
        parseAndExpectError("""fun f(a as string, b as string) -> string := a - b""", "cannot subtract string and string")
    }

    @Test
    fun shouldNotOrderStrings() {
        parseAndExpectError("""fun f(a as string, b as string) -> bool := a < b""", ORDERING_MESSAGE)
        parseAndExpectError("""fun f(a as string, b as string) -> bool := a > b""", ORDERING_MESSAGE)
        parseAndExpectError("""fun f(a as string, b as string) -> bool := a <= b""", ORDERING_MESSAGE)
        parseAndExpectError("""fun f(a as string, b as string) -> bool := a >= b""", ORDERING_MESSAGE)
    }

    @Test
    fun shouldReportMismatchedIfBranchesInStringFunction() {
        // No type mediates between string and a number, so this is the mismatch a string program
        // hits first. The then branch is a parameter reference on purpose: the error path used to
        // hand the enclosing function definition the *unparsed* if-expression, whose identifier had
        // no type yet, and asking that tree for its type crashed the compiler before any error was
        // printed. One error, and it names both branch types.
        parseAndExpectError(
            """
            fun other(s as string) -> i64 := 1
            fun go(s as string, b as bool) -> string := if b then s else other(s)
            """.trimIndent(),
            "both branches of an if expression must have the same type, found: string and i64"
        )
        assertEquals(1, errorListener.errors.size, "expected a single error, found: " + errorListener.errors)
    }

    @Test
    fun shouldNotAssignNumberToString() {
        parseAndExpectError("""val s as string := 17""", "you cannot initialize value 's' of type string with an expression of type i64")
    }

    @Test
    fun shouldNotAssignStringToNumber() {
        parseAndExpectError("""val n as i64 := "17"""", "you cannot initialize value 'n' of type i64 with an expression of type string")
    }

    @Test
    fun shouldNotCastStringToNumber() {
        parseAndExpectError("""fun f(s as string) -> i64 := i64(s)""", "found no match for function call: i64(string)")
        parseAndExpectError("""fun f(s as string) -> f64 := f64(s)""", "found no match for function call: f64(string)")
    }

    @Test
    fun shouldReportUnknownEscape() {
        parseAndExpectError("""val s := "a\qb"""", "unknown escape")
    }

    @Test
    fun shouldReportMalformedCodePointEscape() {
        parseAndExpectError("""val s := "\u{zz}"""", "is not a hexadecimal unicode codepoint")
        parseAndExpectError("""val s := "\uABC"""", "a unicode escape must be written")
    }

    @Test
    fun shouldReportInvalidCodePoint() {
        parseAndExpectError("""val s := "\u{110000}"""", "is not a valid unicode scalar value")
        parseAndExpectError("""val s := "\u{D800}"""", "is not a valid unicode scalar value")
    }

    @Test
    fun shouldReportNulInString() {
        parseAndExpectError("""val s := "a\u{0}b"""", "cannot contain the NUL character")
    }

    @Test
    fun shouldReportEveryMalformedStringInOneCompile() {
        parseAndExpectErrors(
            """
            val a := "x\qy"
            val b := "\u{110000}"
            """.trimIndent(),
            "unknown escape",
            "is not a valid unicode scalar value"
        )
    }

    companion object {
        private const val ORDERING_MESSAGE = "cannot order strings: only == and != are defined for strings"
    }
}
