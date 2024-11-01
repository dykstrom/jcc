/*
 * Copyright (C) 2024 Johan Dykstrom
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
import se.dykstrom.jcc.col.ast.AliasStatement
import se.dykstrom.jcc.col.ast.ImportStatement
import se.dykstrom.jcc.col.ast.PrintlnStatement
import se.dykstrom.jcc.col.compiler.ColTests.Companion.EXT_FUN_FOO
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FL_1_0
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_F64_TO_I64
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_SUM0
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_SUM1
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_SUM2
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_TO_F64
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_TO_I64
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IDE_F64_F
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IDE_I64_A
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IDE_I64_B
import se.dykstrom.jcc.col.compiler.ColTests.Companion.verify
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.ast.IntegerLiteral.ONE
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier

class ColSemanticsParserUserFunctionTests : AbstractColSemanticsParserTests() {

    @BeforeEach
    fun setUp() {
        symbolTable.addFunction(FUN_SUM0)
        symbolTable.addFunction(FUN_SUM1)
        symbolTable.addFunction(FUN_SUM2)
    }

    @Test
    fun shouldParseFunction() {
        // Given
        val identifier = Identifier("foo", FUN_TO_I64)
        val statement = FunctionDefinitionStatement(0, 0, identifier, listOf(), ZERO)

        // When
        val program = parse("fun foo() -> i64 = 0")

        // Then
        verify(program, statement)
        val definedFunction = symbolTable.getFunction("foo", listOf())
        assertEquals(I64.INSTANCE, definedFunction.returnType)
    }

    @Test
    fun shouldParseFunctionWithAliasType() {
        // Given
        val als = AliasStatement(0, 0, "bar", I64.INSTANCE)

        val identifier = Identifier("foo", FUN_TO_I64)
        val fds = FunctionDefinitionStatement(0, 0, identifier, listOf(), ZERO)

        // When
        val program = parse("""
            alias bar as i64
            fun foo() -> bar = 0
            """
        )

        // Then
        verify(program, als, fds)
        val definedFunction = symbolTable.getFunction("foo", listOf())
        assertEquals(I64.INSTANCE, definedFunction.returnType)
    }

    @Test
    fun shouldParseFunctionWithArgs() {
        // Given
        val argTypes = listOf(I64.INSTANCE, F64.INSTANCE)
        val returnType = I64.INSTANCE
        val identifier = Identifier("foo", Fun.from(argTypes, returnType))
        val declarations = listOf(
            Declaration(0, 0, "a", I64.INSTANCE),
            Declaration(0, 0, "b", F64.INSTANCE)
        )
        val expression = ZERO
        val statement = FunctionDefinitionStatement(0, 0, identifier, declarations, expression)

        // When
        val program = parse("fun foo(a as i64, b as f64) -> i64 = 0")

        // Then
        verify(program, statement)
        val definedFunction = symbolTable.getFunction("foo", argTypes)
        assertEquals(returnType, definedFunction.returnType)
    }

    @Test
    fun shouldParseAliasFunctionTypeOneArg() {
        // Given
        val statement = AliasStatement(0, 0, "foo", Fun.from(listOf(F64.INSTANCE), I64.INSTANCE))

        // When
        val program = parse("alias foo as (f64) -> i64")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseFunctionWithFunctionTypeArg() {
        // Given
        val als = AliasStatement(0, 0, "F1", FUN_F64_TO_I64)

        val argTypes = listOf(FUN_TO_F64, FUN_F64_TO_I64)
        val identifier = Identifier("foo", Fun.from(argTypes, I64.INSTANCE))
        val declarations = listOf(
            Declaration(0, 0, "a", FUN_TO_F64),
            Declaration(0, 0, "b", FUN_F64_TO_I64)
        )
        val expression = ZERO
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, expression)

        // When
        val program = parse("""
            alias F1 as (f64) -> i64
            fun foo(a as () -> f64, b as F1) -> i64 = 0
            """)

        // Then
        verify(program, als, fds)
        val definedFunction = symbolTable.getFunction("foo", argTypes)
        assertEquals(I64.INSTANCE, definedFunction.returnType)
    }

    @Test
    fun shouldParseCallToExpressionFunction() {
        // Given
        val identifier = Identifier("foo", FUN_TO_I64)
        val fds = FunctionDefinitionStatement(0, 0, identifier, listOf(), ZERO)

        val fce = FunctionCallExpression(0, 0, identifier, listOf())
        val ps = PrintlnStatement(0, 0, fce)

        // When
        val program = parse("""
            fun foo() -> i64 = 0
            println foo()
            """)

        // Then
        verify(program, fds, ps)
    }

    @Test
    fun shouldParseCallToFunctionWithFunctionTypeArg() {
        // Given
        val identifierFoo = Identifier("foo", Fun.from(listOf(FUN_TO_F64), I64.INSTANCE))
        val declarationsFoo = listOf(Declaration(0, 0, "a", FUN_TO_F64))
        val fdsFoo = FunctionDefinitionStatement(0, 0, identifierFoo, declarationsFoo, ZERO)

        val identifierBar = Identifier("bar", Fun.from(listOf(), F64.INSTANCE))
        val fdsBar = FunctionDefinitionStatement(0, 0, identifierBar, listOf(), FL_1_0)

        val args = listOf(IdentifierDerefExpression(0, 0, identifierBar))
        val ps = PrintlnStatement(0, 0, FunctionCallExpression(0, 0, identifierFoo, args))

        // When
        val program = parse("""
            fun foo(a as () -> f64) -> i64 = 0
            fun bar() -> f64 = 1.0
            println foo(bar)
            """)

        // Then
        verify(program, fdsFoo, fdsBar, ps)
        val definedFunction = symbolTable.getFunction("foo", listOf(FUN_TO_F64))
        assertEquals(I64.INSTANCE, definedFunction.returnType)
    }

    @Test
    fun shouldParseCallToFunctionWithFunctionTypeArgWithSeveralPossibleCandidates() {
        // Given
        val identifierFoo = Identifier("foo", Fun.from(listOf(FUN_TO_F64), I64.INSTANCE))
        val declarationsFoo = listOf(Declaration(0, 0, "a", FUN_TO_F64))
        val fdsFoo = FunctionDefinitionStatement(0, 0, identifierFoo, declarationsFoo, ZERO)

        val identifierBar0 = Identifier("bar", Fun.from(listOf(), F64.INSTANCE))
        val fdsBar0 = FunctionDefinitionStatement(0, 0, identifierBar0, listOf(), FL_1_0)

        val identifierBar1 = Identifier("bar", Fun.from(listOf(I64.INSTANCE), F64.INSTANCE))
        val declarationsBar1 = listOf(Declaration(0, 0, "b", I64.INSTANCE))
        val fdsBar1 = FunctionDefinitionStatement(0, 0, identifierBar1, declarationsBar1, FL_1_0)

        val args = listOf(IdentifierDerefExpression(0, 0, identifierBar0))
        val ps = PrintlnStatement(0, 0, FunctionCallExpression(0, 0, identifierFoo, args))

        // When
        val program = parse("""
            fun foo(a as () -> f64) -> i64 = 0
            fun bar() -> f64 = 1.0
            fun bar(b as i64) -> f64 = 1.0
            println foo(bar)
            """)

        // Then
        verify(program, fdsFoo, fdsBar0, fdsBar1, ps)
        val definedFunction = symbolTable.getFunction("foo", listOf(FUN_TO_F64))
        assertEquals(I64.INSTANCE, definedFunction.returnType)
    }

    @Test
    fun shouldParseFunctionWithFunctionCallExpression() {
        // Given
        val identifier = Identifier("foo", FUN_TO_I64)
        val expression = SubExpression(
            0,
            0,
            FunctionCallExpression(0, 0, FUN_SUM1.identifier, listOf(ZERO)),
            ZERO
        )
        val statement = FunctionDefinitionStatement(0, 0, identifier, listOf(), expression)

        // When
        val program = parse("fun foo() -> i64 = sum(0) - 0")

        // Then
        verify(program, statement)
        val definedFunction = symbolTable.getFunction("foo", listOf())
        assertEquals(I64.INSTANCE, definedFunction.returnType)
    }

    @Test
    fun shouldParseFunctionWithCallToFunctionAbove() {
        // Given
        val identBar = Identifier("bar", Fun.from(listOf(), I64.INSTANCE))
        val fdsBar = FunctionDefinitionStatement(0, 0, identBar, listOf(), ONE)

        val identFoo = Identifier("foo", Fun.from(listOf(), I64.INSTANCE))
        val exprFoo = FunctionCallExpression(0, 0, identBar, listOf())
        val fdsFoo = FunctionDefinitionStatement(0, 0, identFoo, listOf(), exprFoo)

        // When
        val program = parse("""
            fun bar() -> i64 = 1
            fun foo() -> i64 = bar()
            """)

        // Then
        verify(program, fdsBar, fdsFoo)
        val definedBar = symbolTable.getFunction("bar", listOf())
        assertEquals(I64.INSTANCE, definedBar.returnType)
        val definedFoo = symbolTable.getFunction("foo", listOf())
        assertEquals(I64.INSTANCE, definedFoo.returnType)
    }

    @Test
    fun shouldParseFunctionWithCallToFunctionBelow() {
        // Given
        val identBar = Identifier("bar", Fun.from(listOf(), I64.INSTANCE))
        val fdsBar = FunctionDefinitionStatement(0, 0, identBar, listOf(), ONE)

        val identFoo = Identifier("foo", Fun.from(listOf(), I64.INSTANCE))
        val exprFoo = FunctionCallExpression(0, 0, identBar, listOf())
        val fdsFoo = FunctionDefinitionStatement(0, 0, identFoo, listOf(), exprFoo)

        // When
        val program = parse("""
            // Function foo calls function bar that is defined after foo
            fun foo() -> i64 = bar()
            fun bar() -> i64 = 1
            """)

        // Then
        verify(program, fdsFoo, fdsBar)
        val definedBar = symbolTable.getFunction("bar", listOf())
        assertEquals(I64.INSTANCE, definedBar.returnType)
        val definedFoo = symbolTable.getFunction("foo", listOf())
        assertEquals(I64.INSTANCE, definedFoo.returnType)
    }

    @Test
    fun shouldParseFunctionThatCallsItself() {
        // Given
        val identifier = Identifier("foo", Fun.from(listOf(), I64.INSTANCE))
        val expression = FunctionCallExpression(0, 0, identifier, listOf())
        val fds = FunctionDefinitionStatement(0, 0, identifier, listOf(), expression)

        // When
        val program = parse("fun foo() -> i64 = foo()")

        // Then
        verify(program, fds)
        val definedFunction = symbolTable.getFunction("foo", listOf())
        assertEquals(I64.INSTANCE, definedFunction.returnType)
    }

    @Test
    fun shouldParseFunctionThatUsesArgs() {
        // Given
        val argTypes = listOf(I64.INSTANCE, I64.INSTANCE)
        val returnType = I64.INSTANCE
        val identifier = Identifier("foo", Fun.from(argTypes, returnType))
        val declarations = listOf(
            Declaration(0, 0, "a", I64.INSTANCE),
            Declaration(0, 0, "b", I64.INSTANCE)
        )
        val expression = AddExpression(0, 0, IDE_I64_A, IDE_I64_B)
        val statement = FunctionDefinitionStatement(0, 0, identifier, declarations, expression)

        // When
        val program = parse("fun foo(a as i64, b as i64) -> i64 = a + b")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseFunctionThatUsesFloatArgs() {
        // Given
        val argTypes = listOf(F64.INSTANCE)
        val returnType = F64.INSTANCE
        val identifier = Identifier("foo", Fun.from(argTypes, returnType))
        val declarations = listOf(Declaration(0, 0, "f", F64.INSTANCE))
        val statement = FunctionDefinitionStatement(0, 0, identifier, declarations, IDE_F64_F)

        // When
        val program = parse("fun foo(f as f64) -> f64 = f")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseFunctionThatUsesFunctionTypeArg() {
        // Given
        val identifierBar = Identifier("bar", FUN_TO_I64)
        val fceBar = FunctionCallExpression(0, 0, identifierBar, listOf())

        val identifierFoo = Identifier("foo", Fun.from(listOf(FUN_TO_I64), I64.INSTANCE))
        val declarationsFoo = listOf(Declaration(0, 0, "bar", FUN_TO_I64))
        val fdsFoo = FunctionDefinitionStatement(0, 0, identifierFoo, declarationsFoo, fceBar)

        // When
        val program = parse("fun foo(bar as () -> i64) -> i64 = bar()")

        // Then
        verify(program, fdsFoo)
        val definedFoo = symbolTable.getFunction("foo", listOf(FUN_TO_I64))
        assertEquals(I64.INSTANCE, definedFoo.returnType)
    }

    @Test
    fun shouldParseFunctionThatReturnsFunctionTypeArg() {
        // Given
        val identifierBar = Identifier("bar", FUN_TO_I64)
        val ideBar = IdentifierDerefExpression(0, 0, identifierBar)

        val identifierFoo = Identifier("foo", Fun.from(listOf(FUN_TO_I64), FUN_TO_I64))
        val declarationsFoo = listOf(Declaration(0, 0, "bar", FUN_TO_I64))
        val fdsFoo = FunctionDefinitionStatement(0, 0, identifierFoo, declarationsFoo, ideBar)

        // When
        val program = parse("fun foo(bar as () -> i64) -> () -> i64 = bar")

        // Then
        verify(program, fdsFoo)
        val definedFoo = symbolTable.getFunction("foo", listOf(FUN_TO_I64))
        assertEquals(FUN_TO_I64, definedFoo.returnType)
    }

    @Test
    fun shouldParseOverloadedFunction() {
        // Given
        val imsArgTypes = listOf(I64.INSTANCE)
        val libFunction = LibraryFunction("foo", imsArgTypes, I64.INSTANCE, "lib.dll", EXT_FUN_FOO)
        val ims = ImportStatement(0, 0, libFunction)

        val fdsArgTypes = listOf(I64.INSTANCE, I64.INSTANCE)
        val identifier = Identifier("foo", Fun.from(fdsArgTypes, I64.INSTANCE))
        val declarations = listOf(
            Declaration(0, 0, "a", I64.INSTANCE),
            Declaration(0, 0, "b", I64.INSTANCE)
        )
        val expression = AddExpression(0, 0, IDE_I64_A, IDE_I64_B)
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, expression)

        // When
        val program = parse("""
            import lib.foo(i64) -> i64
            fun foo(a as i64, b as i64) -> i64 = a + b
            """)

        // Then
        verify(program, ims, fds)
        val importedFunction = symbolTable.getFunction("foo", imsArgTypes)
        assertEquals(imsArgTypes, importedFunction.argTypes)
        assertEquals(I64.INSTANCE, importedFunction.returnType)
        val userFunction = symbolTable.getFunction("foo", fdsArgTypes)
        assertEquals(fdsArgTypes, userFunction.argTypes)
        assertEquals(I64.INSTANCE, userFunction.returnType)
    }

    @Test
    fun shouldNotParseFunctionWithUnknownArgType() {
        parseAndExpectError("fun foo(x as bar) -> i64 = 0", "undefined type: bar")
    }

    @Test
    fun shouldNotParseFunctionWithUnknownReturnType() {
        parseAndExpectError("fun foo() -> bar = 0", "undefined type: bar")
    }

    @Test
    fun shouldNotParseFunctionWithInvalidExpression() {
        parseAndExpectError("fun foo() -> i64 = bar(0)", "undefined function: bar")
    }

    @Test
    fun shouldNotParseAlreadyDefinedBuiltInFunction() {
        parseAndExpectError("fun sum() -> i64 = 1", "function 'sum() -> i64' has")
    }

    @Test
    fun shouldNotParseAlreadyDefinedUserFunction() {
        parseAndExpectError("""
            fun foo() -> i64 = 1
            fun foo() -> i64 = 2
            """, "function 'foo() -> i64' has")
    }

    @Test
    fun shouldNotParseAlreadyImportedFunction() {
        parseAndExpectError("""
            import lib.foo() -> i64
            fun foo() -> i64 = 2
            """, "function 'foo() -> i64' has")
    }

    @Test
    fun shouldNotParseUndefinedVariable() {
        parseAndExpectError("fun foo() -> i64 = x", "undefined variable: x")
    }

    @Test
    fun shouldNotParseDuplicateArgs() {
        parseAndExpectError("fun foo(a as i64, a as f64) -> i64 = 0", "parameter 'a' is already defined")
    }

    @Test
    fun shouldNotParseCallWithAmbiguousFunctionTypeArgs() {
        parseAndExpectError(
            """
            fun foo(a as () -> f64) -> i64 = 0
            fun foo(a as (i64) -> f64) -> i64 = 0
            fun bar() -> f64 = 0
            fun bar(b as i64) -> f64 = 0
            println foo(bar)
            """,
            "ambiguous function call: foo"
        )
    }

    @Test
    fun shouldNotParseAddFunctions() {
        parseAndExpectError(
            """
            fun foo(a as () -> f64) -> i64 = 0
            fun bar() -> f64 = 0
            println foo(bar + bar)
            """,
            "illegal expression: bar + bar"
        )
    }
}
