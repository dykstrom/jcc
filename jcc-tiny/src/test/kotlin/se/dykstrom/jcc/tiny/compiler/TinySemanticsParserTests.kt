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
import se.dykstrom.jcc.common.error.*
import se.dykstrom.jcc.common.symbols.SymbolTable
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
        parse("BEGIN READ n WRITE n END")
        assertEquals(1, symbolTable.size())
        assertTrue(symbolTable.contains(NAME_N))
    }

    @Test
    fun testAssignment() {
        parse("BEGIN a := 0 END")
        assertEquals(1, symbolTable.size())
        assertTrue(symbolTable.contains(NAME_A))
    }

    @Test
    fun testReadAssignWrite() {
        parse("BEGIN READ a b := a + 1 WRITE b END")
        assertEquals(2, symbolTable.size())
        assertTrue(symbolTable.contains(NAME_A, NAME_B))
    }

    @Test
    fun testMultipleArgs() {
        parse("BEGIN READ a, b c := a + b WRITE a, b, c END")
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

    /**
     * A variable that is assigned but never used should produce an unused-variable warning.
     */
    @Test
    fun testUnusedAssignedVariable() {
        parse("BEGIN a := 0 END")
        assertEquals(1, errorListener.warnings.size)
        val warning = errorListener.warnings[0]
        assertEquals(Warning.UNUSED_VARIABLE, warning.warning)
        assertTrue(warning.msg.contains("unused variable: a"))
    }

    /**
     * A variable that is read but never used should produce an unused-variable warning.
     */
    @Test
    fun testUnusedReadVariable() {
        parse("BEGIN READ a, b WRITE a END")
        assertEquals(1, errorListener.warnings.size)
        val warning = errorListener.warnings[0]
        assertEquals(Warning.UNUSED_VARIABLE, warning.warning)
        assertTrue(warning.msg.contains("unused variable: b"))
    }

    /**
     * A variable that is used should not produce any warning.
     */
    @Test
    fun testNoWarningForUsedVariable() {
        parse("BEGIN READ a b := a + 1 WRITE b END")
        assertTrue(errorListener.warnings.isEmpty())
    }

    private fun parse(text: String) {
        val baseErrorListener = Antlr4Utils.asBaseErrorListener(errorListener)
        val lexer = TinyLexer(CharStreams.fromString(text))
        lexer.removeErrorListeners()
        lexer.addErrorListener(baseErrorListener)
        val parser = TinyParser(CommonTokenStream(lexer))
        parser.removeErrorListeners()
        parser.addErrorListener(baseErrorListener)
        val ctx = parser.program()
        Antlr4Utils.checkParsingComplete(parser)
        val visitor = TinySyntaxVisitor()
        val program = visitor.visitProgram(ctx) as AstProgram
        semanticsParser.parse(program)
    }
}
