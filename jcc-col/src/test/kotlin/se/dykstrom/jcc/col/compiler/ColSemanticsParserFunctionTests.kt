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

import org.junit.Before
import org.junit.Test
import se.dykstrom.jcc.col.ast.FunCallStatement
import se.dykstrom.jcc.col.ast.ImportStatement
import se.dykstrom.jcc.col.ast.PrintlnStatement
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_SUM0
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_SUM1
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_SUM2
import se.dykstrom.jcc.col.compiler.ColTests.Companion.verify
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.ast.SubExpression
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.types.*
import kotlin.test.assertEquals

class ColSemanticsParserFunctionTests : AbstractColSemanticsParserTests() {

    @Before
    fun setUp() {
        symbolTable.addFunction(FUN_SUM0)
        symbolTable.addFunction(FUN_SUM1)
        symbolTable.addFunction(FUN_SUM2)
    }

    @Test
    fun shouldParsePrintlnFunctionCall0() {
        // Given
        val ident = Identifier(FUN_SUM0.name, Fun.from(FUN_SUM0.argTypes, FUN_SUM0.returnType))
        val functionCall = FunctionCallExpression(0, 0, ident, listOf())
        val statement = PrintlnStatement(0, 0, functionCall)

        // When
        val program = parse("println sum()")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnFunctionCall1() {
        // Given
        val ident = Identifier(FUN_SUM1.name, Fun.from(FUN_SUM1.argTypes, FUN_SUM1.returnType))
        val functionCall = FunctionCallExpression(0, 0, ident, listOf(IntegerLiteral.ZERO))
        val statement = PrintlnStatement(0, 0, functionCall)

        // When
        val program = parse("println sum(0)")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnFunctionCall2() {
        // Given
        val ident = Identifier(FUN_SUM2.name, Fun.from(FUN_SUM2.argTypes, FUN_SUM2.returnType))
        val subExpression = SubExpression(0, 0, IntegerLiteral.ZERO, IntegerLiteral.ONE)
        val functionCall = FunctionCallExpression(0, 0, ident, listOf(IntegerLiteral.ZERO, subExpression))
        val statement = PrintlnStatement(0, 0, functionCall)

        // When
        val program = parse("println sum(0, 0 - 1)")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseStandAloneFunctionCall0() {
        // Given
        val ident = Identifier(FUN_SUM0.name, Fun.from(FUN_SUM0.argTypes, FUN_SUM0.returnType))
        val functionCall = FunctionCallExpression(0, 0, ident, listOf())
        val statement = FunCallStatement(0, 0, functionCall)

        // When
        val program = parse("sum()")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseImport() {
        // Given
        val returnType = Void.INSTANCE
        val extFunction = ExternalFunction("foo")
        val libFunction = LibraryFunction("foo", listOf(), returnType, "lib.dll", extFunction)
        val statement = ImportStatement(0, 0, libFunction)

        // When
        val program = parse("import lib.foo()")

        // Then
        verify(program, statement)
        val definedFunction = symbolTable.getFunction("foo", listOf())
        assertEquals(returnType, definedFunction.returnType)
    }

    @Test
    fun shouldParseImportWithReturnType() {
        // Given
        val returnType = I64.INSTANCE
        val extFunction = ExternalFunction("foo")
        val libFunction = LibraryFunction("foo", listOf(), returnType, "lib.dll", extFunction)
        val statement = ImportStatement(0, 0, libFunction)

        // When
        val program = parse("import lib.foo() -> i64")

        // Then
        verify(program, statement)
        val definedFunction = symbolTable.getFunction("foo", listOf())
        assertEquals(returnType, definedFunction.returnType)
    }

    @Test
    fun shouldParseImportWithInternalName() {
        // Given
        val returnType = I64.INSTANCE
        val extFunction = ExternalFunction("foo")
        val libFunction = LibraryFunction("bar", listOf(), returnType, "lib.dll", extFunction)
        val statement = ImportStatement(0, 0, libFunction)

        // When
        val program = parse("import lib.foo() -> i64 as bar")

        // Then
        verify(program, statement)
        val definedFunction = symbolTable.getFunction("bar", listOf())
        assertEquals(returnType, definedFunction.returnType)
    }

    @Test
    fun shouldParseImportWithOneArg() {
        // Given
        val argTypes = listOf(I64.INSTANCE)
        val returnType = I64.INSTANCE
        val extFunction = ExternalFunction("foo")
        val libFunction = LibraryFunction("foo", argTypes, returnType, "lib.dll", extFunction)
        val statement = ImportStatement(0, 0, libFunction)

        // When
        val program = parse("import lib.foo(i64) -> i64")

        // Then
        verify(program, statement)
        val definedFunction = symbolTable.getFunction("foo", listOf(I64.INSTANCE))
        assertEquals(argTypes, definedFunction.argTypes)
        assertEquals(returnType, definedFunction.returnType)
    }

    @Test
    fun shouldParseImportWithTwoArgs() {
        // Given
        val argTypes = listOf(I64.INSTANCE, I64.INSTANCE)
        val returnType = I64.INSTANCE
        val extFunction = ExternalFunction("foo")
        val libFunction = LibraryFunction("bar", argTypes, returnType, "lib.dll", extFunction)
        val statement = ImportStatement(0, 0, libFunction)

        // When
        val program = parse("import lib.foo(i64, i64) -> i64 as bar")

        // Then
        verify(program, statement)
        val definedFunction = symbolTable.getFunction("bar", listOf(I64.INSTANCE, I64.INSTANCE))
        assertEquals(argTypes, definedFunction.argTypes)
        assertEquals(returnType, definedFunction.returnType)
    }

    @Test
    fun shouldParsePrintlnCallToImportedFunction() {
        // Given
        val returnType = I64.INSTANCE
        val argTypes = listOf<Type>()

        val extFunction = ExternalFunction("foo")
        val libFunction = LibraryFunction("foo", argTypes, returnType, "lib.dll", extFunction)
        val importStatement = ImportStatement(0, 0, libFunction)

        val ident = Identifier("foo", Fun.from(argTypes, returnType))
        val fce = FunctionCallExpression(0, 0, ident, listOf())
        val printlnStatement = PrintlnStatement(0, 0, fce)

        // When
        val program = parse("""
            import lib.foo() -> i64
            println foo()
            """)

        // Then
        verify(program, importStatement, printlnStatement)
    }

    @Test
    fun shouldParseStandAloneCallToImportedFunction() {
        // Given
        val returnType = I64.INSTANCE
        val argTypes = listOf<Type>()

        val extFunction = ExternalFunction("foo")
        val libFunction = LibraryFunction("foo", argTypes, returnType, "lib.dll", extFunction)
        val importStatement = ImportStatement(0, 0, libFunction)

        val ident = Identifier("foo", Fun.from(argTypes, returnType))
        val fce = FunctionCallExpression(0, 0, ident, listOf())
        val funCallStatement = FunCallStatement(0, 0, fce)

        // When
        val program = parse("""
            import lib.foo() -> i64
            foo()
            """)

        // Then
        verify(program, importStatement, funCallStatement)
    }

    @Test
    fun shouldNotParseUnknownFunctionCall() {
        parseAndExpectError("println foo()", "undefined function: foo")
    }

    @Test
    fun shouldNotParseImportWithUndefinedReturnType() {
        parseAndExpectError("import lib.foo() -> bar", "undefined type: bar")
    }

    @Test
    fun shouldNotParseImportWithUndefinedArgType() {
        parseAndExpectError("import lib.foo(bar)", "undefined type: bar")
    }

    @Test
    fun shouldNotParseImportOfAlreadyDefinedFunction() {
        parseAndExpectError("import lib.sum()", "function 'sum()' has")
    }

    @Test
    fun shouldNotParseImportOfAlreadyDefinedFunctionWithArgs() {
        parseAndExpectError("import lib.sum(i64, i64)", "function 'sum(i64, i64)' has")
    }
}
