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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.assembly.directive.Label
import se.dykstrom.jcc.common.assembly.instruction.*
import se.dykstrom.jcc.common.assembly.macro.Import
import se.dykstrom.jcc.common.assembly.macro.Library
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.compiler.DefaultTypeManager
import se.dykstrom.jcc.common.code.TargetProgram
import se.dykstrom.jcc.common.code.Line
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.tiny.ast.ReadStatement
import se.dykstrom.jcc.tiny.ast.WriteStatement
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IDENT_A
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IDENT_B
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IDE_A
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IDE_B
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IL_1
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IL_17
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IL_2
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IL_23
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.IL_5
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.INE_A
import se.dykstrom.jcc.tiny.compiler.AbstractTinyTests.Companion.INE_B
import java.nio.file.Path

class TinyCodeGeneratorTests {

    private val sourcePath = Path.of("file.tiny")

    private val typeManager = DefaultTypeManager()
    private val symbolTable = SymbolTable()
    private val optimizer = DefaultAstOptimizer(typeManager, symbolTable)
    private val codeGenerator = TinyCodeGenerator(typeManager, symbolTable, optimizer)

    @Test
    fun testEmptyProgram() {
        val result = assembleProgram(emptyList())
        val dependencies = codeGenerator.dependencies()
        assertEquals(1, dependencies.size)
        val library = dependencies.values.iterator().next()
        assertTrue(library.contains("exit"))
        assertCodeLines(result.lines(), 1)
    }

    @Test
    fun testSingleReadSingleIdentifier() {
        val statement: Statement = ReadStatement(0, 0, listOf(IDENT_A))
        val result = assembleProgram(listOf(statement))
        val dependencies = codeGenerator.dependencies()
        assertEquals(1, dependencies.size)
        val library = dependencies.values.iterator().next()
        assertTrue(library.contains("exit"))
        assertTrue(library.contains("scanf"))
        assertCodeLines(result.lines(), 2)
    }

    @Test
    fun testSingleReadMultipleIdentifiers() {
        val statement: Statement = ReadStatement(0, 0, listOf(IDENT_A, IDENT_B))
        val result = assembleProgram(listOf(statement))
        val dependencies = codeGenerator.dependencies()
        assertEquals(1, dependencies.size)
        val library = dependencies.values.iterator().next()
        assertTrue(library.contains("exit"))
        assertTrue(library.contains("scanf"))
        assertCodeLines(result.lines(), 3)
    }

    @Test
    fun testMultipleReadsSingleIdentifier() {
        val statement1: Statement = ReadStatement(1, 0, listOf(IDENT_A))
        val statement2: Statement = ReadStatement(2, 0, listOf(IDENT_B))
        val result = assembleProgram(listOf(statement1, statement2))
        val dependencies = codeGenerator.dependencies()
        assertEquals(1, dependencies.size)
        val library = dependencies.values.iterator().next()
        assertTrue(library.contains("exit"))
        assertTrue(library.contains("scanf"))
        assertCodeLines(result.lines(), 3)
    }

    @Test
    fun testSingleAssignmentLiteralExpression() {
        val statement: Statement = AssignStatement(0, 0, INE_A, IL_5)
        val result = assembleProgram(listOf(statement))
        val lines = result.lines()
        assertEquals(2, lines.filterIsInstance<MoveImmToReg>().count())
        assertEquals(1, lines.filterIsInstance<MoveRegToMem>().count())
    }

    @Test
    fun testSingleAssignmentIdentifierExpression() {
        val statement = AssignStatement(0, 0, INE_A, IDE_B)
        val result = assembleProgram(listOf(statement))
        val lines = result.lines()
        assertEquals(1, lines.filterIsInstance<MoveImmToReg>().count())
        assertEquals(1, lines
            .filterIsInstance<MoveMemToReg>()
            .map { code -> code.source }
            .count { name -> name == "[" + IDENT_B.mappedName + "]" })
        assertEquals(1, lines
            .filterIsInstance<MoveRegToMem>()
            .map { code -> code.destination }
            .count { name -> name == "[" + IDENT_A.mappedName + "]" })
    }

    @Test
    fun testSingleAssignmentAddExpression() {
        val expression: Expression = AddExpression(0, 0, IL_1, IL_2)
        val statement: Statement = AssignStatement(0, 0, INE_A, expression)
        val result = assembleProgram(listOf(statement))
        val lines = result.lines()
        assertEquals(1, lines.filterIsInstance<AddRegToReg>().count())
        assertEquals(1, lines
            .filterIsInstance<MoveRegToMem>()
            .map { code -> code.destination }
            .count { name -> name == "[" + IDENT_A.mappedName + "]" })
    }

    @Test
    fun testSingleAssignmentSubExpression() {
        val expression: Expression = SubExpression(0, 0, IL_17, IL_5)
        val statement: Statement = AssignStatement(0, 0, INE_A, expression)
        val result = assembleProgram(listOf(statement))
        val lines = result.lines()
        assertEquals(1, lines.filterIsInstance<SubRegFromReg>().count())
        assertEquals(1, lines
            .filterIsInstance<MoveRegToMem>()
            .map { code -> code.destination }
            .count { name -> name == "[" + IDENT_A.mappedName + "]" })
    }

    @Test
    fun testMultipleAssignmentsLiteralExpression() {
        val statement0 = AssignStatement(0, 0, INE_A, IL_5)
        val statement1 = AssignStatement(1, 0, INE_B, IL_23)
        val result = assembleProgram(listOf(statement0, statement1))
        val lines = result.lines()
        assertEquals(3, lines.filterIsInstance<MoveImmToReg>().count())
        assertEquals(2, lines.filterIsInstance<MoveRegToMem>().count())
    }

    @Test
    fun testSingleWriteSingleExpression() {
        val statement = WriteStatement(0, 0, listOf(IL_2))
        val result = assembleProgram(listOf(statement))
        val lines = result.lines()
        assertEquals(3, lines.filterIsInstance<MoveImmToReg>().count())
    }

    @Test
    fun testSingleWriteAddExpression() {
        val expression = AddExpression(0, 0, IL_1, IL_2)
        val statement = WriteStatement(0, 0, listOf(expression))
        val result = assembleProgram(listOf(statement))
        val lines = result.lines()
        assertEquals(4, lines.filterIsInstance<MoveImmToReg>().count())
        assertEquals(1, lines.filterIsInstance<AddRegToReg>().count())
    }

    @Test
    fun testSingleWriteWithExpressionList() {
        val expression0 = AddExpression(0, 0, IL_1, IL_2)
        val expression1 = AddExpression(0, 0, IL_5, IL_17)
        val statement = WriteStatement(0, 0, listOf(expression0, expression1))
        val result = assembleProgram(listOf(statement))
        val lines = result.lines()
        assertEquals(7, lines.filterIsInstance<MoveImmToReg>().count())
        assertEquals(2, lines.filterIsInstance<AddRegToReg>().count())
    }

    @Test
    fun testReadAssignWrite() {
        val readStatement = ReadStatement(1, 0, listOf(IDENT_A))
        val assignExpression = AddExpression(2, 0, IDE_A, IL_1)
        val assignStatement = AssignStatement(2, 0, INE_B, assignExpression)
        val writeExpression = IdentifierDerefExpression(3, 0, IDENT_B)
        val writeStatement = WriteStatement(3, 0, listOf(writeExpression))
        val result = assembleProgram(listOf(readStatement, assignStatement, writeStatement))
        val lines = result.lines()
        assertCodeLines(lines, 3)
        assertEquals(1, lines.filterIsInstance<AddRegToReg>().count())
        assertEquals(5, lines.filterIsInstance<MoveImmToReg>().count())
        assertEquals(2, lines.filterIsInstance<MoveMemToReg>().count())
        assertEquals(1, lines.filterIsInstance<MoveRegToMem>().count())
    }

    private fun assembleProgram(statements: List<Statement>): TargetProgram {
        val program = AstProgram(0, 0, statements).withSourcePath(sourcePath)
        return codeGenerator.generate(program)
    }

    private fun assertCodeLines(lines: List<Line>, calls: Int) {
        assertEquals(1, lines.filterIsInstance<Library>().count())
        assertEquals(1, lines.filterIsInstance<Import>().count())
        assertEquals(1, lines.filterIsInstance<Label>().count())
        assertEquals(calls, lines.filterIsInstance<Call>().count())
    }
}
