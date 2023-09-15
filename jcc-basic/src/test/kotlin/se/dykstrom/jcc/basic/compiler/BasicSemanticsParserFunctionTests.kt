/*
 * Copyright (C) 2017 Johan Dykstrom
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
import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.*
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.types.*
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests class `BasicSemanticsParser`, especially functionality related to function calls.
 *
 * @author Johan Dykstrom
 * @see BasicSemanticsParser
 */
class BasicSemanticsParserFunctionTests : AbstractBasicSemanticsParserTests() {

    @Before
    fun setUp() {
        // Define some functions for testing
        defineFunction(FUN_ABS)
        defineFunction(FUN_COMMAND)
        defineFunction(FUN_FMOD)
        defineFunction(FUN_INSTR2)
        defineFunction(FUN_INSTR3)
        // Function 'sum' is overloaded with different number of arguments
        defineFunction(FUN_SUM1)
        defineFunction(FUN_SUM2)
        defineFunction(FUN_SUM3)
        // Function 'lbound' takes a generic array as argument
        defineFunction(FUN_LBOUND)
        defineFunction(FUN_LBOUND_I64)
    }

    @Test
    fun shouldParseCall() {
        parse("let a% = abs(1)")
        parse("let c$ = command$()")
        parse("let f = fmod(1.0, 2.0)")
    }

    @Test
    fun shouldParseOverloadedFunctionCall() {
        parse("let a% = sum(1)")
        parse("let a% = sum(1, 2)")
        parse("let a% = sum(1, 2, 3)")
    }

    @Test
    fun shouldParseCallWithoutParens() {
        parse("let c$ = command$")
    }

    @Test
    fun shouldParseCallWithTypeCastArguments() {
        parse("let f = fmod(1.0, 2)")
        parse("let f = fmod(1, 2.0)")
        parse("let f = fmod(1, 2)")
    }

    @Test
    fun shouldParseCallWithTypeCastReturnValue() {
        parse("let a% = fmod(1.0, 2.0)")
        parse("dim b as integer : b = fmod(1.0, 2.0)")
        parse("let f# = abs(7)")
        parse("dim g as double : g = abs(7)")
    }

    @Test
    fun shouldParseFunctionCallAsFactor() {
        parse("let a% = sum(-1) * 55")
    }

    @Test
    fun shouldParseCallAndFindType() {
        // Given
        val expression = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf(IL_1))
        val assignStatement = AssignStatement(0, 0, INE_I64_A, expression)
        val expectedStatements = listOf(assignStatement)

        // When
        val program = parse("a% = abs(1)")

        // Then
        assertEquals(expectedStatements, program.statements)
    }

    @Test
    fun shouldParseCallWithFunCallArgs() {
        // Given
        val fe1 = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf(IL_1))
        val fe2 = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf<Expression>(fe1))
        val fe3 = FunctionCallExpression(0, 0, FUN_ABS.identifier, listOf<Expression>(fe2))
        val assignStatement = AssignStatement(0, 0, INE_I64_A, fe3)
        val expectedStatements = listOf(assignStatement)

        // When
        val program = parse("let a% = abs(abs(abs(1)))")

        // Then
        assertEquals(expectedStatements, program.statements)
    }

    @Test
    fun shouldParseFunctionCallWithUndefinedVariable() {
        parse("let a% = sum(b%)")
        parse("let a% = sum(h%, i%, j%)")
        parse("let f# = fmod(s#, t#)")
        parse("let f# = fmod(s, t)")
    }

    @Test
    fun shouldParseFunctionCallWithTypedVariable() {
        parse("defint b : let a% = sum(b)")
        parse("defint b-d : let a% = sum(b, c, d)")
        parse("defdbl s-u, v-z : let f = fmod(u, v)")
    }

    @Test
    fun shouldReplaceIdeWithIne() {
        // Given
        symbolTable.addArray(IDENT_ARR_I64_X, DECL_ARR_I64_X)
        val originalArgs = listOf(IDE_F64_X)

        // When
        val updatedArgs = semanticsParser.replaceIdesWithInesForArrays(originalArgs, symbolTable)

        // Then
        assertEquals(1, updatedArgs.size)
        assertEquals(INE_ARR_I64_X, updatedArgs[0])
    }

    @Test
    fun shouldReplaceSeveralIdesWithInes() {
        // Given
        symbolTable.addArray(IDENT_ARR_I64_X, DECL_ARR_I64_X)
        val originalArgs = listOf(IDE_F64_X, IL_2, INE_ARR_I64_X)

        // When
        val updatedArgs = semanticsParser.replaceIdesWithInesForArrays(originalArgs, symbolTable)

        // Then
        assertEquals(3, updatedArgs.size)
        assertEquals(INE_ARR_I64_X, updatedArgs[0])
        assertEquals(IL_2, updatedArgs[1])
        assertEquals(INE_ARR_I64_X, updatedArgs[2])
    }

    @Test
    fun shouldParseCallWithArrayArgument() {
        // Given
        val dimStatement = VariableDeclarationStatement(0, 0, listOf(DECL_ARR_I64_X))
        val functionCallExpression = FunctionCallExpression(0, 0, FUN_LBOUND.identifier, listOf(INE_ARR_I64_X))
        val assignStatement = AssignStatement(0, 0, INE_I64_A, functionCallExpression)
        val expectedStatements = listOf(dimStatement, assignStatement)

        // When
        val program = parse(
            """
            dim x(3) as integer
            a% = lbound(x)
            """
        )

        // Then
        assertEquals(expectedStatements, program.statements)
    }

    @Test
    fun shouldParseCallWithArrayArgumentWithNameClash() {
        // Given
        val dimStatement0 = VariableDeclarationStatement(0, 0, listOf(DECL_STR_X))
        val dimStatement1 = VariableDeclarationStatement(0, 0, listOf(DECL_ARR_I64_X))
        val functionCallExpression = FunctionCallExpression(0, 0, FUN_LBOUND.identifier, listOf(INE_ARR_I64_X))
        val assignStatement = AssignStatement(0, 0, INE_I64_A, functionCallExpression)
        val expectedStatements = listOf(dimStatement0, dimStatement1, assignStatement)

        // When
        val program = parse(
            """
            dim x as string
            dim x(3) as integer
            a% = lbound(x)
            """
        )

        // Then
        assertEquals(expectedStatements, program.statements)
    }

    @Test
    fun shouldParseCallWithArrayAndIntegerArguments() {
        // Given
        val dimStatement0 = VariableDeclarationStatement(0, 0, listOf(DECL_STR_X))
        val dimStatement1 = VariableDeclarationStatement(0, 0, listOf(DECL_ARR_I64_X))
        val functionCallExpression = FunctionCallExpression(0, 0, FUN_LBOUND_I64.identifier, listOf(INE_ARR_I64_X, IL_1))
        val assignStatement = AssignStatement(0, 0, INE_I64_A, functionCallExpression)
        val expectedStatements = listOf(dimStatement0, dimStatement1, assignStatement)

        // When
        val program = parse(
            """
            dim x as string
            dim x(3) as integer
            a% = lbound(x, 1)
            """
        )

        // Then
        assertEquals(expectedStatements, program.statements)
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
    fun shouldNotParseCallToUndefined() {
        parseAndExpectException("print foo(1, 2, 3)", "undefined function")
    }

    @Test
    fun shouldNotParseCallToVariable() {
        parseAndExpectException("let a = 5 print a()", "undefined function")
    }

    @Test
    fun shouldNotParseCallWithWrongReturnType() {
        parseAndExpectException("let number% = command$", "a value of type string")
    }

    @Test
    fun shouldNotParseCallWithWrongNumberOfArgs() {
        parseAndExpectException("print abs(1, 2)", "found no match for function call: abs(integer, integer)")
        parseAndExpectException("print command$(1)", "found no match for function call: command$(integer)")
        parseAndExpectException("print sum()", "found no match for function call: sum()")
    }

    @Test
    fun shouldNotParseCallWithWrongArgTypes() {
        parseAndExpectException("print abs(\"-1\")", "found no match for function call: abs(string)")
        parseAndExpectException("print fmod(\"-1\", 1.0)", "found no match for function call: fmod(string, double)")
        parseAndExpectException("print sum(1, \"\", 0)", "found no match for function call: sum(integer, string, integer)")
    }

    @Test
    fun shouldNotParseCallWithDefaultWrongArgTypes() {
        parseAndExpectException("foo = instr(x, y)", "found no match for function call: instr(double, double)")
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
            DEF FNbar(x AS STRING) = 1
            """,
            "function 'FNbar(Str) : F64'")
    }
}
