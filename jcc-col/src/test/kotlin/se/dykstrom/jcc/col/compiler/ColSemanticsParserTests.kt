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

import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import se.dykstrom.jcc.col.ast.AliasStatement
import se.dykstrom.jcc.col.ast.PrintlnStatement
import se.dykstrom.jcc.col.types.ColTypeManager
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.error.SemanticsException
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets.UTF_8
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ColSemanticsParserTests {

    private val typeManager = ColTypeManager()

    private val symbolTable = SymbolTable()

    private val errorListener = CompilationErrorListener()

    private val syntaxParser = ColSyntaxParser(errorListener)

    private val semanticsParser = ColSemanticsParser(errorListener, symbolTable, typeManager)

    @Before
    fun setUp() {
        symbolTable.addFunction(FUN_SUM0)
        symbolTable.addFunction(FUN_SUM1)
        symbolTable.addFunction(FUN_SUM2)
    }

    @Test
    fun shouldParseEmptyPrintln() {
        // Given
        val statement = PrintlnStatement(0, 0, null)

        // When
        val program = parse("println")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnLiteral() {
        // Given
        val statement = PrintlnStatement(0, 0, IntegerLiteral.ONE)

        // When
        val program = parse("println 1")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldPrintMaxI64() {
        // Given
        val statement = PrintlnStatement(0, 0, IntegerLiteral(0, 0, "9223372036854775807"))

        // When
        val program = parse("println 9223372036854775807")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParsePrintlnAdd() {
        // Given
        val expression = AddExpression(0, 0, IntegerLiteral.ONE, IntegerLiteral.ZERO)
        val statement = PrintlnStatement(0, 0, expression)

        // When
        val program = parse("println 1 + 0")

        // Then
        verify(program, statement)
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
    fun shouldParseAliasI64() {
        // Given
        val statement = AliasStatement(0, 0, "foo", I64.INSTANCE)

        // When
        val program = parse("alias foo = i64")

        // Then
        verify(program, statement)
    }

    @Test
    fun shouldParseAliasOfAlias() {
        // Given
        val as1 = AliasStatement(0, 0, "foo", I64.INSTANCE)
        val as2 = AliasStatement(0, 0, "bar", I64.INSTANCE)

        // When
        val program = parse(
            """
                alias foo = i64
                alias bar = foo
                """
        )

        // Then
        verify(program, as1, as2)
    }

    @Test
    fun shouldNotParseUnknownFunctionCall() {
        parseAndExpectError("println foo()", "undefined function: foo")
    }

    @Test
    fun shouldNotParseUnknownAliasType() {
        parseAndExpectError("alias foo = bar", "undefined type: bar")
    }

    @Test
    fun shouldNotParseRedefineType() {
        parseAndExpectError("alias i64 = i64", "cannot redefine type: i64")
    }

    @Test
    fun shouldNotParseIntegerOverflow() {
        val value = "9223372036854775808"
        parseAndExpectError("println $value", "integer out of range: $value")
    }

    private fun parse(text: String): Program {
        val parsedProgram = syntaxParser.parse(ByteArrayInputStream(text.toByteArray(UTF_8)))
        assertFalse { errorListener.hasErrors() }
        val checkedProgram = semanticsParser.parse(parsedProgram)
        assertFalse { errorListener.hasErrors() }
        return checkedProgram
    }

    private fun parseAndExpectError(text: String, errorText: String) {
        assertThrows(SemanticsException::class.java) {
            semanticsParser.parse(syntaxParser.parse(ByteArrayInputStream(text.toByteArray(UTF_8))))
        }
        assertTrue { errorListener.hasErrors() }
        assertTrue { errorListener.errors.any { it.msg.contains(errorText) } }
    }

    private fun verify(program: Program, vararg statements: Statement) {
        assertEquals(statements.size, program.statements.size)
        for ((index, statement) in statements.withIndex()) {
            assertEquals(statement, program.statements[index])
        }
    }

    companion object {
        private val FUN_SUM0 = LibraryFunction("sum", listOf(), I64.INSTANCE, "", ExternalFunction(""))
        private val FUN_SUM1 = LibraryFunction("sum", listOf(I64.INSTANCE), I64.INSTANCE, "", ExternalFunction(""))
        private val FUN_SUM2 = LibraryFunction("sum", listOf(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE, "", ExternalFunction(""))
    }
}
