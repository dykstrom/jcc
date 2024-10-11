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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.antlr4.Antlr4Utils
import se.dykstrom.jcc.assembunny.ast.JnzStatement
import se.dykstrom.jcc.assembunny.compiler.AssembunnyTests.Companion.IDE_A
import se.dykstrom.jcc.assembunny.compiler.AssembunnyTests.Companion.INE_A
import se.dykstrom.jcc.assembunny.types.AssembunnyTypeManager
import se.dykstrom.jcc.common.ast.AstProgram
import se.dykstrom.jcc.common.ast.IncStatement
import se.dykstrom.jcc.common.ast.LabelledStatement
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.symbols.SymbolTable

class AssembunnySemanticsParserTests {

    private val errorListener = CompilationErrorListener()
    private val symbolTable = SymbolTable()
    private val typeManager = AssembunnyTypeManager()

    @Test
    fun shouldParseInc() {
        // When
        val program = parse("inc a")

        // Then
        assertFalse(errorListener.hasErrors())
        assertEquals(1, program.statements.size)
    }

    @Test
    fun shouldParseCorrectJnz() {
        // Given
        val incStatement = IncStatement(0, 0, INE_A)
        val js = JnzStatement(0, 0, IDE_A, "line0")

        // When
        val program = parse("inc a jnz a -1")

        // Then
        assertFalse(errorListener.hasErrors())
        assertEquals(2, program.statements.size)
        assertEquals(LabelledStatement("line0", incStatement), program.statements[0])
        assertEquals(LabelledStatement("line1", js), program.statements[1])
    }

    @Test
    fun shouldParseInvalidJnz() {
        // Given
        val incStatement = IncStatement(0, 0, INE_A)
        val js = JnzStatement(0, 0, IDE_A, AssembunnyUtils.END_JUMP_TARGET)

        // When
        val program = parse("inc a jnz a 5")

        // Then
        assertFalse(errorListener.hasErrors())
        assertEquals(2, program.statements.size)
        assertEquals(LabelledStatement("line0", incStatement), program.statements[0])
        assertEquals(LabelledStatement("line1", js), program.statements[1])
    }

    private fun parse(text: String): AstProgram {
        val baseErrorListener = Antlr4Utils.asBaseErrorListener(errorListener)

        val lexer = AssembunnyLexer(CharStreams.fromString(text))
        lexer.addErrorListener(baseErrorListener)
        val syntaxParser = AssembunnyParser(CommonTokenStream(lexer))
        syntaxParser.addErrorListener(baseErrorListener)

        val ctx = syntaxParser.program()
        Antlr4Utils.checkParsingComplete(syntaxParser)

        val visitor = AssembunnySyntaxVisitor()
        val program = visitor.visitProgram(ctx) as AstProgram
        val semanticsParser = AssembunnySemanticsParser(errorListener, symbolTable, typeManager)
        return semanticsParser.parse(program)
    }
}
