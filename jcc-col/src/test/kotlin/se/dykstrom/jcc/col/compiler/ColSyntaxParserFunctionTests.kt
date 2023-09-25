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

import org.junit.Test
import se.dykstrom.jcc.col.ast.FunCallStatement
import se.dykstrom.jcc.col.ast.ImportStatement
import se.dykstrom.jcc.col.ast.PrintlnStatement
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_5
import se.dykstrom.jcc.col.compiler.ColTests.Companion.verify
import se.dykstrom.jcc.col.types.NamedType
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.ast.SubExpression
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.Identifier

class ColSyntaxParserFunctionTests : AbstractColSyntaxParserTests() {

    @Test
    fun shouldParsePrintlnFunctionCall0() {
        // Given
        val ident = Identifier("foo", Fun.from(listOf(), null))
        val functionCall = FunctionCallExpression(0, 0, ident, listOf())
        val statement = PrintlnStatement(0, 0, functionCall)

        // When
        val program = parse("println foo()")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnFunctionCall1() {
        // Given
        val ident = Identifier("foo", Fun.from(listOf(null), null))
        val functionCall = FunctionCallExpression(0, 0, ident, listOf(IL_5))
        val statement = PrintlnStatement(0, 0, functionCall)

        // When
        val program = parse("println foo(5)")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnFunctionCall2() {
        // Given
        val ident = Identifier("foo", Fun.from(listOf(null, null), null))
        val subExpression = SubExpression(0, 0, IntegerLiteral.ZERO, IntegerLiteral.ONE)
        val functionCall = FunctionCallExpression(0, 0, ident, listOf(IL_5, subExpression))
        val statement = PrintlnStatement(0, 0, functionCall)

        // When
        val program = parse("println foo(5, 0 - 1)")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseStandAloneFunctionCall() {
        // Given
        val ident = Identifier("foo", Fun.from(listOf(), null))
        val functionCall = FunctionCallExpression(0, 0, ident, listOf())
        val statement = FunCallStatement(0, 0, functionCall)

        // When
        val program = parse("foo()")

        // Then
        verify(program, statement)
    }

    /**
     * This will be parsed as 'println foo()'.
     */
    @Test
    fun shouldParseEmptyPrintlnFollowedByFunctionCall() {
        // Given
        val ident = Identifier("foo", Fun.from(listOf(), null))
        val functionCall = FunctionCallExpression(0, 0, ident, listOf())
        val statement = PrintlnStatement(0, 0, functionCall)

        // When
        val program = parse("""
            println
            foo()
            """)

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseImport() {
        // Given
        val returnType = NamedType("void")
        val extFunction = ExternalFunction("foo")
        val libFunction = LibraryFunction("foo", listOf(), returnType, "lib", extFunction)
        val statement = ImportStatement(0, 0, libFunction)

        // When
        val program = parse("import lib.foo()")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseImportWithReturnType() {
        // Given
        val returnType = NamedType("i64")
        val extFunction = ExternalFunction("foo")
        val libFunction = LibraryFunction("foo", listOf(), returnType, "lib", extFunction)
        val statement = ImportStatement(0, 0, libFunction)

        // When
        val program = parse("import lib.foo() -> i64")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseImportWithInternalName() {
        // Given
        val returnType = NamedType("i64")
        val extFunction = ExternalFunction("foo")
        val libFunction = LibraryFunction("bar", listOf(), returnType, "lib", extFunction)
        val statement = ImportStatement(0, 0, libFunction)

        // When
        val program = parse("import lib.foo() -> i64 as bar")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseImportWithUnderscore() {
        // Given
        val argTypes = listOf(NamedType("i64"))
        val returnType = NamedType("i64")
        val extFunction = ExternalFunction("_abs64")
        val libFunction = LibraryFunction("abs", argTypes, returnType, "msvcrt", extFunction)
        val statement = ImportStatement(0, 0, libFunction)

        // When
        val program = parse("import msvcrt._abs64(i64) -> i64 as abs")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseImportWithOneArg() {
        // Given
        val argTypes = listOf(NamedType("i64"))
        val returnType = NamedType("i64")
        val extFunction = ExternalFunction("foo")
        val libFunction = LibraryFunction("foo", argTypes, returnType, "lib", extFunction)
        val statement = ImportStatement(0, 0, libFunction)

        // When
        val program = parse("import lib.foo(i64) -> i64")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseImportWithThreeArgs() {
        // Given
        val argTypes = listOf(NamedType("i64"), NamedType("i64"), NamedType("i64"))
        val returnType = NamedType("i64")
        val extFunction = ExternalFunction("foo")
        val libFunction = LibraryFunction("bar", argTypes, returnType, "lib", extFunction)
        val statement = ImportStatement(0, 0, libFunction)

        // When
        val program = parse("import lib.foo(i64, i64, i64) -> i64 as bar")

        // Then
        verify(program, statement)
    }
}
