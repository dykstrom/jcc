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
package se.dykstrom.jcc.tiny.compiler

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.Test
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.AssignStatement
import se.dykstrom.jcc.common.ast.Program
import se.dykstrom.jcc.common.ast.SubExpression
import se.dykstrom.jcc.common.utils.ParseUtils
import se.dykstrom.jcc.tiny.ast.ReadStatement
import se.dykstrom.jcc.tiny.ast.WriteStatement
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IDENT_A
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IDENT_B
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IDENT_N
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IDE_A
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IDE_B
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IDE_C
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IDE_N
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IL_0
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IL_1
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IL_17
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IL_M3
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.NE_A
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.NE_B
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.NE_C
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.SYNTAX_ERROR_LISTENER
import kotlin.test.assertEquals

class TinySyntaxVisitorTests {

    @Test
    fun testWrite() {
        val ws = WriteStatement(0, 0, listOf(IL_17))
        val program = parse("BEGIN WRITE 17 END")
        val statements = program.statements
        assertEquals(1, statements.size)
        assertEquals(ws, statements[0])
    }

    @Test
    fun testReadWrite() {
        val rs = ReadStatement(0, 0, listOf(IDENT_N))
        val ws = WriteStatement(0, 0, listOf(IDE_N))
        val program = parse("BEGIN READ n WRITE n END")
        val statements = program.statements
        assertEquals(2, statements.size)
        assertEquals(rs, statements[0])
        assertEquals(ws, statements[1])
    }

    @Test
    fun testAssignment() {
        val ass = AssignStatement(0, 0, NE_A, IL_0)
        val program = parse("BEGIN a := 0 END")
        val statements = program.statements
        assertEquals(1, statements.size)
        assertEquals(ass, statements[0])
    }

    @Test
    fun testReadAssignWrite() {
        val rs = ReadStatement(0, 0, listOf(IDENT_A))
        val ae = AddExpression(0, 0, IDE_A, IL_1)
        val ass = AssignStatement(0, 0, NE_B, ae)
        val ws = WriteStatement(0, 0, listOf(IDE_B))
        val program = parse("BEGIN READ a b := a + 1 WRITE b END")
        val statements = program.statements
        assertEquals(3, statements.size)
        assertEquals(rs, statements[0])
        assertEquals(ass, statements[1])
        assertEquals(ws, statements[2])
    }

    @Test
    fun testMultipleArgs() {
        val rs = ReadStatement(0, 0, listOf(IDENT_A, IDENT_B))
        val ass = AssignStatement(0, 0, NE_C, AddExpression(0, 0, IDE_A, IDE_B))
        val ws = WriteStatement(0, 0, listOf(IDE_A, IDE_B, IDE_C))
        val program = parse("BEGIN READ a, b c := a + b WRITE a, b, c END")
        val statements = program.statements
        assertEquals(3, statements.size)
        assertEquals(rs, statements[0])
        assertEquals(ass, statements[1])
        assertEquals(ws, statements[2])
    }

    @Test
    fun testMultipleAssignments() {
        val rs = ReadStatement(0, 0, listOf(IDENT_A))
        val as1 = AssignStatement(0, 0, NE_B, AddExpression(0, 0, IDE_A, IL_1))
        val as2 = AssignStatement(0, 0, NE_C, SubExpression(0, 0, IDE_B, IL_1))
        val ws = WriteStatement(0, 0, listOf(IDE_A, IDE_B, IDE_C))
        val program = parse("""
            |BEGIN 
            |  READ a 
            |  b := a + 1 
            |  c := b - 1 
            |  WRITE a, b, c 
            |END
            |""".trimMargin()
        )
        val statements = program.statements
        assertEquals(4, statements.size)
        assertEquals(rs, statements[0])
        assertEquals(as1, statements[1])
        assertEquals(as2, statements[2])
        assertEquals(ws, statements[3])
    }

    @Test
    fun testNegativeNumber() {
        val ass = AssignStatement(0, 0, NE_A, IL_M3)
        val ws = WriteStatement(0, 0, listOf(IDE_A))
        val expectedStatements = listOf(ass, ws)
        val program = parse("BEGIN a := -3 WRITE a END")
        val actualStatements = program.statements
        assertEquals(2, actualStatements.size)
        assertEquals(expectedStatements, actualStatements)
    }

    private fun parse(text: String): Program {
        val lexer = TinyLexer(CharStreams.fromString(text))
        lexer.addErrorListener(SYNTAX_ERROR_LISTENER)
        val parser = TinyParser(CommonTokenStream(lexer))
        parser.addErrorListener(SYNTAX_ERROR_LISTENER)
        val ctx = parser.program()
        ParseUtils.checkParsingComplete(parser)
        val visitor = TinySyntaxVisitor()
        return visitor.visitProgram(ctx) as Program
    }
}
