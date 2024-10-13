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

package se.dykstrom.jcc.assembunny.compiler

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.antlr4.Antlr4Utils
import se.dykstrom.jcc.assembunny.ast.CpyStatement
import se.dykstrom.jcc.assembunny.ast.JnzStatement
import se.dykstrom.jcc.assembunny.ast.OutnStatement
import se.dykstrom.jcc.assembunny.compiler.AssembunnyTests.Companion.ERROR_LISTENER
import se.dykstrom.jcc.assembunny.compiler.AssembunnyTests.Companion.IDE_A
import se.dykstrom.jcc.assembunny.compiler.AssembunnyTests.Companion.IDE_B
import se.dykstrom.jcc.assembunny.compiler.AssembunnyTests.Companion.IL_1
import se.dykstrom.jcc.assembunny.compiler.AssembunnyTests.Companion.INE_A
import se.dykstrom.jcc.assembunny.compiler.AssembunnyTests.Companion.INE_B
import se.dykstrom.jcc.assembunny.compiler.AssembunnyTests.Companion.INE_C
import se.dykstrom.jcc.common.ast.*

/**
 * Tests class `AssembunnySyntaxVisitor`.
 *
 * @author Johan Dykstrom
 * @see AssembunnySyntaxVisitor
 */
class AssembunnySyntaxVisitorTests {

    @Test
    fun shouldParseEmptyProgram() {
        parseAndAssert("", emptyList())
    }

    @Test
    fun shouldParseInc() {
        val incStatement = IncStatement(0, 0, INE_A)
        val expectedStatements = listOf(LabelledStatement("line0", incStatement))
        parseAndAssert("inc a", expectedStatements)
    }

    @Test
    fun shouldParseDec() {
        val ds = DecStatement(0, 0, INE_B)
        val expectedStatements = listOf(LabelledStatement("line0", ds))
        parseAndAssert("dec b", expectedStatements)
    }

    @Test
    fun shouldParseCpyFromReg() {
        val cs = CpyStatement(0, 0, IDE_A, INE_B)
        val expectedStatements = listOf(LabelledStatement("line0", cs))
        parseAndAssert("cpy a b", expectedStatements)
    }

    @Test
    fun shouldParseCpyFromInt() {
        val cs = CpyStatement(0, 0, IL_1, INE_C)
        val expectedStatements = listOf(LabelledStatement("line0", cs))
        parseAndAssert("cpy 1 c", expectedStatements)
    }

    @Test
    fun shouldParseJnzOnReg() {
        val js = JnzStatement(0, 0, IDE_A, "line3")
        val expectedStatements = listOf(LabelledStatement("line0", js))
        parseAndAssert("jnz a 3", expectedStatements)
    }

    @Test
    fun shouldParseJnzOnInt() {
        val inc0 = IncStatement(0, 0, INE_A)
        val inc1 = IncStatement(0, 0, INE_A)
        val jnz2 = JnzStatement(0, 0, IL_1, "line0")
        val expectedStatements = listOf(
            LabelledStatement("line0", inc0),
            LabelledStatement("line1", inc1),
            LabelledStatement("line2", jnz2),
        )
        parseAndAssert("inc a inc a jnz 1 -2", expectedStatements)
    }

    @Test
    fun shouldParseOutn() {
        val os = OutnStatement(0, 0, IDE_B)
        val expectedStatements = listOf(LabelledStatement("line0", os))
        parseAndAssert("outn b", expectedStatements)
    }

    @Test
    fun shouldParseMultipleStatements() {
        val incStatement = IncStatement(0, 0, INE_A)
        val ds1 = DecStatement(0, 0, INE_A)
        val cs = CpyStatement(0, 0, IL_1, INE_B)
        val ds2 = DecStatement(0, 0, INE_B)
        // A relative jump of -1 from 4 is an absolute jump to 3
        val js = JnzStatement(0, 0, IDE_B, "line3")
        val os = OutnStatement(0, 0, IDE_A)

        val expectedStatements = listOf(
            LabelledStatement("line0", incStatement),
            LabelledStatement("line1", ds1),
            LabelledStatement("line2", cs),
            LabelledStatement("line3", ds2),
            LabelledStatement("line4", js),
            LabelledStatement("line5", os)
        )

        parseAndAssert(
            """
                inc a
                dec a
                cpy 1 b
                dec b
                jnz b -1
                outn a
                """, expectedStatements
        )
    }

    /**
     * Parses the given program text, and asserts that the parsed text and the given statements are equal.
     *
     * @param text The code in text form.
     * @param expectedStatements The code in AST form.
     */
    private fun parseAndAssert(text: String, expectedStatements: List<Statement>) {
        val program = parse(text)
        assertEquals(expectedStatements, program.statements)
    }

    /**
     * Parses the given program text, and returns the AST for the parsed program.
     */
    private fun parse(text: String): AstProgram {
        val lexer = AssembunnyLexer(CharStreams.fromString(text))
        lexer.addErrorListener(ERROR_LISTENER)
        val parser = AssembunnyParser(CommonTokenStream(lexer))
        parser.addErrorListener(ERROR_LISTENER)
        val ctx = parser.program()
        Antlr4Utils.checkParsingComplete(parser)
        val visitor = AssembunnySyntaxVisitor()
        return visitor.visitProgram(ctx) as AstProgram
    }
}
