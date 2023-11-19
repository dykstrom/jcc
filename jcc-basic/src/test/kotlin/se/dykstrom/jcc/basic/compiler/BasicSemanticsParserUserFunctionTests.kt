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

package se.dykstrom.jcc.basic.compiler

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import se.dykstrom.jcc.basic.ast.PrintStatement
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.FL_2_0
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.FL_3_14
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.FUN_F64_TO_F64
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.FUN_I64_F64_TO_F64
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.FUN_STR_TO_STR
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.FUN_TO_F64
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.FUN_TO_STR
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.IL_1
import se.dykstrom.jcc.basic.compiler.BasicTests.Companion.SL_A
import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_ABS
import se.dykstrom.jcc.common.ast.Declaration
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement
import se.dykstrom.jcc.common.ast.VariableDeclarationStatement
import se.dykstrom.jcc.common.types.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests class `BasicSemanticsParser`, especially functionality related to function definitions.
 *
 * @author Johan Dykstrom
 * @see BasicSemanticsParser
 */
class BasicSemanticsParserUserFunctionTests : AbstractBasicSemanticsParserTests() {

    @Before
    fun setUp() {
        // Define some functions for testing
        defineFunction(FUN_ABS)
    }

    @Test
    fun shouldParseNoArgDefFnExpression() {
        // Given
        val ident = Identifier("FNbar", FUN_TO_F64)
        val fds = FunctionDefinitionStatement(0, 0, ident, listOf(), IL_1)
        val expectedStatements = listOf(fds)

        // When
        val program = parse("DEF FNbar() = 1")

        // Then
        assertEquals(expectedStatements, program.statements)
    }

    @Test
    fun shouldParseNoArgDefFnExpressionWithTypeSpecifier() {
        // Given
        val ident = Identifier("FNbar$", FUN_TO_STR)
        val fds = FunctionDefinitionStatement(0, 0, ident, listOf(), SL_A)
        val expectedStatements = listOf(fds)

        // When
        val program = parse("DEF FNbar$() = \"A\"")

        // Then
        assertEquals(expectedStatements, program.statements)
    }

    @Test
    fun shouldParseOneArgDefaultDefFnExpression() {
        // Given
        val ident = Identifier("FNfoo", FUN_F64_TO_F64)
        val args = listOf(Declaration(0, 0, "f", F64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, ident, args, IL_1)
        val expectedStatements = listOf(fds)

        // When
        val program = parse("DEF FNfoo(f) = 1")

        // Then
        assertEquals(expectedStatements, program.statements)
    }

    @Test
    fun shouldParseDefFnExpressionWithStringArg() {
        // Given
        val ident = Identifier("FNbar$", FUN_STR_TO_STR)
        val fds = FunctionDefinitionStatement(0, 0, ident, listOf(DECL_STR_X), IDE_STR_X)
        val expectedStatements = listOf(fds)

        // When
        val program = parse("DEF FNbar$(x AS STRING) = x")

        // Then
        assertEquals(expectedStatements, program.statements)
        // Function parameter should not have been added to the global symbol table
        assertFalse { symbolTable.contains("x") }
        // Function should have been added to the global symbol table
        assertTrue { symbolTable.containsFunction("FNbar$") }
    }

    @Test
    fun shouldParseDefFnExpressionWithManyArgs() {
        // Given
        val functionType = Fun.from(listOf(Str.INSTANCE, F64.INSTANCE, I64.INSTANCE), Str.INSTANCE)
        val ident = Identifier("FNbar$", functionType)
        val declarations = listOf(
            Declaration(0, 0, "x", Str.INSTANCE),
            Declaration(0, 0, "y", F64.INSTANCE),
            Declaration(0, 0, "z", I64.INSTANCE)
        )
        val fds = FunctionDefinitionStatement(0, 0, ident, declarations, IDE_STR_X)
        val expectedStatements = listOf(fds)

        // When
        val program = parse("DEF FNbar$(x AS STRING, y AS DOUBLE, z AS INTEGER) = x")

        // Then
        assertEquals(expectedStatements, program.statements)
    }

    @Test
    fun shouldParseDefFnExpressionThatReturnsGlobalVariable() {
        // Given
        val vds = VariableDeclarationStatement(0, 0, listOf(DECL_STR_X))
        val ident = Identifier("FNbar$", FUN_TO_STR)
        val fds = FunctionDefinitionStatement(0, 0, ident, listOf(), IDE_STR_X)
        val expectedStatements = listOf(vds, fds)

        // When
        val program = parse("""
            DIM x AS STRING
            DEF FNbar$() = x
            """)

        // Then
        assertEquals(expectedStatements, program.statements)
        // Variable should have been added to the global symbol table
        assertTrue { symbolTable.contains("x") }
        // Function should have been added to the global symbol table
        assertTrue { symbolTable.containsFunction("FNbar$") }
    }

    @Test
    fun shouldParseCallToDefFnExpression() {
        // Given
        val ident = Identifier("FNbar", FUN_TO_F64)
        val fds = FunctionDefinitionStatement(0, 0, ident, listOf(), IL_1)
        val fce = FunctionCallExpression(0, 0, ident, listOf())
        val ps = PrintStatement(0, 0, listOf(fce))
        val expectedStatements = listOf(fds, ps)

        // When
        val program = parse("""
            DEF FNbar() = 1
            PRINT FNbar()
            """)

        // Then
        assertEquals(expectedStatements, program.statements)
    }

    @Test
    fun oneExpressionFunctionCanCallAnother() {
        // Given
        val identBar = Identifier("FNbar", FUN_TO_F64)
        val fdsBar = FunctionDefinitionStatement(0, 0, identBar, listOf(), IL_1)
        val fce = FunctionCallExpression(0, 0, identBar, listOf())
        val identFoo = Identifier("FNfoo", FUN_TO_F64)
        val fdsFoo = FunctionDefinitionStatement(0, 0, identFoo, listOf(), fce)
        val expectedStatements = listOf(fdsBar, fdsFoo)

        // When
        val program = parse("""
            DEF FNbar() = 1
            DEF FNfoo() = FNbar()
            """)

        // Then
        assertEquals(expectedStatements, program.statements)
        // Functions should have been added to the global symbol table
        assertTrue { symbolTable.containsFunction("FNbar") }
        assertTrue { symbolTable.containsFunction("FNfoo") }
    }

    @Test
    fun twoFunctionsWithSameNameDifferentSignature() {
        // Given
        val ident1 = Identifier("FNbar", FUN_F64_TO_F64)
        val declarations1 = listOf(Declaration(0, 0, "x", F64.INSTANCE))
        val fds1 = FunctionDefinitionStatement(0, 0, ident1, declarations1, FL_2_0)

        val ident2 = Identifier("FNbar", FUN_I64_F64_TO_F64)
        val declarations2 = listOf(
            Declaration(0, 0, "x", I64.INSTANCE),
            Declaration(0, 0, "y", F64.INSTANCE)
        )
        val fds2 = FunctionDefinitionStatement(0, 0, ident2, declarations2, FL_3_14)
        val expectedStatements = listOf(fds1, fds2)

        // When
        val program = parse(
            """
            DEF FNbar(x AS DOUBLE) = 2.0
            DEF FNbar(x AS INTEGER, y AS DOUBLE) = 3.14
            """
        )

        // Then
        assertEquals(expectedStatements, program.statements)
        // Functions should have been added to the global symbol table
        assertTrue { symbolTable.containsFunction("FNbar", listOf(F64.INSTANCE)) }
        assertTrue { symbolTable.containsFunction("FNbar", listOf(I64.INSTANCE, F64.INSTANCE)) }
    }

    @Test
    fun userDefinedFunctionNamesAreCaseSensitive() {
        parseAndExpectException("""
            DEF FNbar() = 0
            PRINT FNBar()
            """,
            "undefined function: FNBar")
    }

    @Test
    fun shouldAlsoValidateDefFnExpression() {
        parseAndExpectException("DEF FNbar() = abs(\"\")", "function call: abs(string)")
    }

    @Test
    fun shouldNotReturnExpressionWithInvalidType() {
        parseAndExpectException("DEF FNbar() = \"\"", "function 'FNbar' with return type double")
    }

    @Test
    fun shouldNotDefineFunctionThatExists() {
        parseAndExpectException("""
            DEF FNbar(x AS STRING) = 0
            DEF FNbar(y AS STRING) = 1
            """,
            "function 'FNbar(Str) -> F64'")
    }

    @Test
    fun argumentsCannotHaveSameNames() {
        parseAndExpectException("DEF FNbar(x AS DOUBLE, x AS INTEGER) = 1.0", "parameter 'x' is already defined")
    }
}
