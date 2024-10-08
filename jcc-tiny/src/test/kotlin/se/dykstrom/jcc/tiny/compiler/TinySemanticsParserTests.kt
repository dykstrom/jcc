/*
 * Copyright (C) 2016 Johan Dykstrom
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
package se.dykstrom.jcc.tiny.compiler

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.antlr4.Antlr4Utils
import se.dykstrom.jcc.common.ast.AstProgram
import se.dykstrom.jcc.common.compiler.DefaultTypeManager
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.error.InvalidValueException
import se.dykstrom.jcc.common.error.SemanticsException
import se.dykstrom.jcc.common.error.UndefinedException
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.utils.FormatUtils.EOL
import se.dykstrom.jcc.tiny.compiler.TinyTests.Companion.NAME_A
import se.dykstrom.jcc.tiny.compiler.TinyTests.Companion.NAME_B
import se.dykstrom.jcc.tiny.compiler.TinyTests.Companion.NAME_C
import se.dykstrom.jcc.tiny.compiler.TinyTests.Companion.NAME_N
import se.dykstrom.jcc.tiny.compiler.TinyTests.Companion.NAME_UNDEFINED

class TinySemanticsParserTests {

    private val errorListener = CompilationErrorListener()
    private val symbolTable = SymbolTable()
    private val typeManager = DefaultTypeManager()

    private val semanticsParser = TinySemanticsParser(errorListener, symbolTable, typeManager)

    @Test
    fun testWrite() {
        parse("BEGIN WRITE 17 END")
        assertEquals(0, symbolTable.size())
    }

    @Test
    fun testReadWrite() {
        parse("BEGIN" + EOL + "READ n" + EOL + "WRITE n" + EOL + "END")
        assertEquals(1, symbolTable.size())
        assertTrue(symbolTable.contains(NAME_N))
    }

    @Test
    fun testAssignment() {
        parse("BEGIN" + EOL + "a := 0" + EOL + "END")
        assertEquals(1, symbolTable.size())
        assertTrue(symbolTable.contains(NAME_A))
    }

    @Test
    fun testReadAssignWrite() {
        parse("BEGIN" + EOL + "READ a" + EOL + "b := a + 1" + EOL + "WRITE b" + EOL + "END")
        assertEquals(2, symbolTable.size())
        assertTrue(symbolTable.contains(NAME_A, NAME_B))
    }

    @Test
    fun testMultipleArgs() {
        parse("BEGIN" + EOL + "READ a, b" + EOL + "c := a + b" + EOL + "WRITE a, b, c" + EOL + "END")
        assertEquals(3, symbolTable.size())
        assertTrue(symbolTable.contains(NAME_A, NAME_B, NAME_C))
    }

    @Test
    fun testMultipleAssignments() {
        parse("""
            |BEGIN
            |  READ a
            |  b := a + 1
            |  c := b - 1
            |  WRITE a, b, c
            |END
            |""".trimMargin()
        )
        assertEquals(3, symbolTable.size())
        assertTrue(symbolTable.contains(NAME_A, NAME_B, NAME_C))
    }

    @Test
    fun testMaxI64() {
        parse("BEGIN WRITE 9223372036854775807 END")
        assertEquals(0, symbolTable.size())
    }

    /**
     * Invalid integer -> overflow.
     */
    @Test
    fun testOverflowI64() {
        val value = "9223372036854775808"
        assertThrows<SemanticsException> { parse("BEGIN WRITE $value END") }
        assertEquals(1, errorListener.errors.size)
        val ive = errorListener.errors[0].exception as InvalidValueException
        assertEquals(value, ive.value())
    }

    /**
     * Undefined identifier in write statement.
     */
    @Test
    fun testUndefinedInWrite() {
        assertThrows<SemanticsException> { parse("BEGIN WRITE undefined END") }
        assertEquals(1, errorListener.errors.size)
        val ue = errorListener.errors[0].exception as UndefinedException
        assertEquals(NAME_UNDEFINED, ue.name)
    }

    /**
     * Undefined identifier in assign statement.
     */
    @Test
    fun testUndefinedInAssign() {
        assertThrows<SemanticsException> { parse("BEGIN a := undefined END") }
        assertEquals(1, errorListener.errors.size)
        val ue = errorListener.errors[0].exception as UndefinedException
        assertEquals(NAME_UNDEFINED, ue.name)
    }

    /**
     * Undefined identifier in complex expression.
     */
    @Test
    fun testUndefinedInExpression() {
        assertThrows<SemanticsException> { parse("BEGIN WRITE 1 + undefined - 2 END") }
        assertEquals(1, errorListener.errors.size)
        val ue = errorListener.errors[0].exception as UndefinedException
        assertEquals(NAME_UNDEFINED, ue.name)
    }

    /**
     * Undefined identifier in expression list.
     */
    @Test
    fun testUndefinedInList() {
        assertThrows<SemanticsException> { parse("BEGIN WRITE 1, undefined, 3 END") }
        assertEquals(1, errorListener.errors.size)
        val ue = errorListener.errors[0].exception as UndefinedException
        assertEquals(NAME_UNDEFINED, ue.name)
    }

    private fun parse(text: String) {
        val baseErrorListener = Antlr4Utils.asBaseErrorListener(errorListener)
        val lexer = TinyLexer(CharStreams.fromString(text))
        lexer.addErrorListener(baseErrorListener)
        val parser = TinyParser(CommonTokenStream(lexer))
        parser.addErrorListener(baseErrorListener)
        val ctx = parser.program()
        Antlr4Utils.checkParsingComplete(parser)
        val visitor = TinySyntaxVisitor()
        val program = visitor.visitProgram(ctx) as AstProgram
        semanticsParser.parse(program)
    }
}
