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

package se.dykstrom.jcc.basic.compiler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.ast.Declaration
import se.dykstrom.jcc.common.ast.VariableDeclarationStatement
import se.dykstrom.jcc.common.error.SemanticsException
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Str

/**
 * Tests class `BasicSemanticsParser`, especially functionality related to the type name
 * in an AS clause. The name is resolved here, not in the grammar, so that an unknown or
 * unsupported name can be named in the error message.
 *
 * @author Johan Dykstrom
 * @see BasicSemanticsParser
 */
class BasicSemanticsParserTypeNameTests : AbstractBasicSemanticsParserTests() {

    @Test
    fun shouldResolveTypeNames() {
        val program = parse("dim a as double, b as integer, c as string")
        val statement = program.statements[0] as VariableDeclarationStatement
        assertEquals(
            listOf(
                Declaration("a", F64.INSTANCE),
                Declaration("b", I64.INSTANCE),
                Declaration("c", Str.INSTANCE)
            ),
            statement.declarations
        )
    }

    @Test
    fun shouldResolveTypeNamesInAnyCase() {
        parse("dim a as DOUBLE, b as Integer, c as string")
        parse("dim arr(5) as INTEGER")
        parse("def FNfoo(x as Double) = x")
    }

    /**
     * DOUBLE, INTEGER and STRING are not reserved words, since any identifier is
     * accepted as a type name.
     */
    @Test
    fun shouldUseTypeNameAsVariableName() {
        parse("double = 5 : print double")
        parse("dim integer as integer : integer = 5 : print integer")
    }

    @Test
    fun shouldNotParseMisspelledTypeName() {
        parseAndExpectException("dim a as DOBLE", "unknown type 'DOBLE'; did you mean 'DOUBLE'?")
        parseAndExpectException("dim a as INTEGR", "unknown type 'INTEGR'; did you mean 'INTEGER'?")
        parseAndExpectException("dim a as strng", "unknown type 'strng'; did you mean 'STRING'?")
    }

    @Test
    fun shouldNotParseUnknownTypeName() {
        parseAndExpectException("dim a as FOO", "unknown type 'FOO'")
        parseAndExpectException("dim a as PERSON", "unknown type 'PERSON'")
    }

    @Test
    fun shouldNotParseUnsupportedTypeName() {
        parseAndExpectException("dim a as SINGLE", "type 'SINGLE' is not supported by JCC; use 'DOUBLE'")
        parseAndExpectException("dim a as LONG", "type 'LONG' is not supported by JCC; use 'INTEGER'")
        parseAndExpectException("dim a as CURRENCY", "type 'CURRENCY' is not supported by JCC; use 'DOUBLE'")
    }

    @Test
    fun shouldNotParseUnsupportedArrayElementType() {
        parseAndExpectException("dim arr(5) as SINGLE", "type 'SINGLE' is not supported by JCC; use 'DOUBLE'")
        parseAndExpectException("dim arr(5) as DOBLE", "unknown type 'DOBLE'; did you mean 'DOUBLE'?")
    }

    @Test
    fun shouldNotParseUnsupportedParameterType() {
        parseAndExpectException("def FNfoo(x as SINGLE) = x", "type 'SINGLE' is not supported by JCC; use 'DOUBLE'")
        parseAndExpectException("def FNfoo(x as DOBLE) = x", "unknown type 'DOBLE'; did you mean 'DOUBLE'?")
    }

    /**
     * A bad type name does not stop the analysis, so several of them are reported in one compile.
     */
    @Test
    fun shouldReportAllBadTypeNames() {
        assertThrows(SemanticsException::class.java) { parse("dim a as SINGLE, b as DOBLE, c as LONG") }
        assertEquals(
            listOf(
                "type 'SINGLE' is not supported by JCC; use 'DOUBLE'",
                "unknown type 'DOBLE'; did you mean 'DOUBLE'?",
                "type 'LONG' is not supported by JCC; use 'INTEGER'"
            ),
            errorListener.errors.map { it.msg }
        )
    }

    /**
     * A declaration whose type name is unknown carries on with the unknown type, so nothing it is
     * used for afterwards is reported against a type the compiler picked for it.
     */
    @Test
    fun shouldNotReportUsesOfAVariableWithAnUnknownTypeName() {
        parseAndExpectException("dim arr(5) as STRONG : arr(0) = \"a\"", "unknown type 'STRONG'")
        assertEquals(1, errorListener.errors.size, errorListener.errors.toString())
    }

    @Test
    fun shouldNotReportUsesOfAScalarWithAnUnknownTypeName() {
        parseAndExpectException("dim a as STRONG : a = \"x\" : print a", "unknown type 'STRONG'")
        assertEquals(1, errorListener.errors.size, errorListener.errors.toString())
    }

    @Test
    fun shouldKeepTheTypeTheNameImpliesAfterAnUnknownTypeName() {
        // The suffix and DEFtype are the programmer's own statements about the type, unlike the
        // default type, so the checks that follow still have something true to work with
        parseAndExpectException("dim a$ as STRONG : a$ = 1", "you cannot assign a value of type integer")
        assertEquals(2, errorListener.errors.size, errorListener.errors.toString())
    }
}
