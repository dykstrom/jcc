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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ast.statement.FunCallStatement
import se.dykstrom.jcc.col.ast.statement.ImportStatement
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_PRINTLN_I64
import se.dykstrom.jcc.col.compiler.ColTests.Companion.EXT_FUN_FOO
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_SUM0
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_SUM1
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_SUM2
import se.dykstrom.jcc.col.compiler.ColTests.Companion.verify
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.ast.SubExpression
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.types.*

class ColSemanticsParserFunctionTests : AbstractColSemanticsParserTests() {

    @BeforeEach
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
        val statement = funCall(BF_PRINTLN_I64, functionCall)

        // When
        val program = parse("call println(sum())")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnFunctionCall1() {
        // Given
        val ident = Identifier(FUN_SUM1.name, Fun.from(FUN_SUM1.argTypes, FUN_SUM1.returnType))
        val functionCall = FunctionCallExpression(0, 0, ident, listOf(IntegerLiteral.ZERO))
        val statement = funCall(BF_PRINTLN_I64, functionCall)

        // When
        val program = parse("call println(sum(0))")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnFunctionCall2() {
        // Given
        val ident = Identifier(FUN_SUM2.name, Fun.from(FUN_SUM2.argTypes, FUN_SUM2.returnType))
        val subExpression = SubExpression(0, 0, IntegerLiteral.ZERO, IntegerLiteral.ONE)
        val functionCall = FunctionCallExpression(0, 0, ident, listOf(IntegerLiteral.ZERO, subExpression))
        val statement = funCall(BF_PRINTLN_I64, functionCall)

        // When
        val program = parse("call println(sum(0, 0 - 1))")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseStandAloneFunctionCall0() {
        // Given
        val ident = Identifier(FUN_SUM0.name, Fun.from(FUN_SUM0.argTypes, FUN_SUM0.returnType))
        val functionCall = FunctionCallExpression(0, 0, ident, listOf())
        val statement = FunCallStatement(functionCall)

        // When
        val program = parse("call sum()")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseImport() {
        // Given
        val returnType = Void.INSTANCE
        val libFunction = LibraryFunction("foo", listOf(), returnType, "lib.dll", EXT_FUN_FOO)
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
        val libFunction = LibraryFunction("foo", listOf(), returnType, "lib.dll", EXT_FUN_FOO)
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
        val libFunction = LibraryFunction("bar", listOf(), returnType, "lib.dll", EXT_FUN_FOO)
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
        val libFunction = LibraryFunction("foo", argTypes, returnType, "lib.dll", EXT_FUN_FOO)
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
        val libFunction = LibraryFunction("bar", argTypes, returnType, "lib.dll", EXT_FUN_FOO)
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
    fun shouldParseImportWithFunctionTypeArg() {
        // Given
        val argTypes = listOf(Fun.from(listOf(), I64.INSTANCE))
        val libFunction = LibraryFunction("foo", argTypes, I64.INSTANCE, "lib.dll", EXT_FUN_FOO)
        val statement = ImportStatement(0, 0, libFunction)

        // When
        val program = parse("import lib.foo(() -> i64) -> i64")

        // Then
        verify(program, statement)
        val definedFunction = symbolTable.getFunction("foo", argTypes)
        assertEquals(argTypes, definedFunction.argTypes)
        assertEquals(I64.INSTANCE, definedFunction.returnType)
    }

    @Test
    fun shouldParsePrintlnCallToImportedFunction() {
        // Given
        val returnType = I64.INSTANCE
        val argTypes = listOf<Type>()

        val libFunction = LibraryFunction("foo", argTypes, returnType, "lib.dll", EXT_FUN_FOO)
        val importStatement = ImportStatement(0, 0, libFunction)

        val ident = Identifier("foo", Fun.from(argTypes, returnType))
        val fce = FunctionCallExpression(0, 0, ident, listOf())
        val printlnStatement = funCall(BF_PRINTLN_I64, fce)

        // When
        val program = parse("""
            import lib.foo() -> i64
            call println(foo())
            """)

        // Then
        verify(program, importStatement, printlnStatement)
    }

    @Test
    fun shouldParseStandAloneCallToImportedFunction() {
        // Given
        val returnType = I64.INSTANCE
        val argTypes = listOf<Type>()

        val libFunction = LibraryFunction("foo", argTypes, returnType, "lib.dll", EXT_FUN_FOO)
        val importStatement = ImportStatement(0, 0, libFunction)

        val ident = Identifier("foo", Fun.from(argTypes, returnType))
        val fce = FunctionCallExpression(0, 0, ident, listOf())
        val funCallStatement = FunCallStatement(0, 0, fce)

        // When
        val program = parse("""
            import lib.foo() -> i64
            call foo()
            """)

        // Then
        verify(program, importStatement, funCallStatement)
    }

    @Test
    fun shouldNotParseUnknownFunctionCall() {
        parseAndExpectError("call println(foo())", "undefined function: foo")
    }

    @Test
    fun shouldNotParseCallWithFloatInsteadOfInt() {
        parseAndExpectError("call println(sum(0.3))", "found no match for function call: sum(f64)")
    }

    @Test
    fun shouldNotParseCallWithIntInsteadOfFloat() {
        parseAndExpectError("call println(sqrt(0))", "found no match for function call: sqrt(i64)")
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
