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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.assembunny.ast.CpyStatement
import se.dykstrom.jcc.assembunny.ast.JnzStatement
import se.dykstrom.jcc.assembunny.ast.OutnStatement
import se.dykstrom.jcc.assembunny.compiler.AssembunnyTests.Companion.IDE_B
import se.dykstrom.jcc.assembunny.compiler.AssembunnyTests.Companion.IL_1
import se.dykstrom.jcc.assembunny.compiler.AssembunnyTests.Companion.INE_D
import se.dykstrom.jcc.assembunny.compiler.AssembunnyUtils.END_JUMP_TARGET
import se.dykstrom.jcc.assembunny.types.AssembunnyTypeManager
import se.dykstrom.jcc.common.assembly.instruction.*
import se.dykstrom.jcc.common.assembly.macro.Import
import se.dykstrom.jcc.common.assembly.macro.Library
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.code.Label
import se.dykstrom.jcc.common.code.Line
import se.dykstrom.jcc.common.code.TargetProgram
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer
import se.dykstrom.jcc.common.symbols.SymbolTable
import java.nio.file.Path

class AssembunnyCodeGeneratorTests {

    private val typeManager = AssembunnyTypeManager()
    private val symbolTable = SymbolTable()
    private val optimizer = DefaultAstOptimizer(typeManager, symbolTable)
    private val codeGenerator = AssembunnyCodeGenerator(typeManager, symbolTable, optimizer)

    @Test
    fun shouldGenerateEmptyProgram() {
        val result = assembleProgram(emptyList())
        assertCodeLines(result.lines(), 1, 1, 2, 1)
        // Initialize registers to 0
        assertEquals(4, countInstances(MoveImmToReg::class.java, result.lines()))
    }

    @Test
    fun shouldGenerateInc() {
        val incStatement = IncStatement(0, 0, INE_D)
        val result = assembleProgram(listOf(LabelledStatement("0", incStatement)))
        assertCodeLines(result.lines(), 1, 1, 3, 1)
        assertEquals(1, countInstances(IncReg::class.java, result.lines()))
    }

    @Test
    fun shouldGenerateDec() {
        val ds = DecStatement(0, 0, INE_D)
        val result = assembleProgram(listOf(LabelledStatement("0", ds)))
        assertCodeLines(result.lines(), 1, 1, 3, 1)
        assertEquals(1, countInstances(DecReg::class.java, result.lines()))
    }

    @Test
    fun shouldGenerateCpyFromInt() {
        val cs = CpyStatement(0, 0, IL_1, INE_D)
        val result = assembleProgram(listOf(LabelledStatement("0", cs)))
        assertCodeLines(result.lines(), 1, 1, 3, 1)
        // Four for initializing, and one for the cpy statement
        assertEquals(5, countInstances(MoveImmToReg::class.java, result.lines()))
    }

    @Test
    fun shouldGenerateCpyFromReg() {
        val cs = CpyStatement(0, 0, IDE_B, INE_D)
        val result = assembleProgram(listOf(LabelledStatement("0", cs)))
        assertCodeLines(result.lines(), 1, 1, 3, 1)
        // Four for initializing
        assertEquals(4, countInstances(MoveImmToReg::class.java, result.lines()))
        // One for the base pointer, one for the cpy statement, and one for the exit statement
        assertEquals(3, countInstances(MoveRegToReg::class.java, result.lines()))
    }

    @Test
    fun shouldGenerateJnzOnInt() {
        val js = JnzStatement(0, 0, IL_1, END_JUMP_TARGET)
        val result = assembleProgram(listOf(LabelledStatement("0", js)))
        assertCodeLines(result.lines(), 1, 1, 3, 1)
        // Four for initializing, one for the integer literal
        assertEquals(5, countInstances(MoveImmToReg::class.java, result.lines()))
        // One for the jnz statement
        assertEquals(1, countInstances(CmpRegWithImm::class.java, result.lines()))
        // One for the jnz statement
        assertEquals(1, countInstances(Jne::class.java, result.lines()))
    }

    @Test
    fun shouldGenerateJnzOnReg() {
        val js = JnzStatement(0, 0, IDE_B, END_JUMP_TARGET)
        val result = assembleProgram(listOf(LabelledStatement("0", js)))
        assertCodeLines(result.lines(), 1, 1, 3, 1)
        // Four for initializing
        assertEquals(4, countInstances(MoveImmToReg::class.java, result.lines()))
        // One for the base pointer, one for the register expression, and one for the exit statement
        assertEquals(3, countInstances(MoveRegToReg::class.java, result.lines()))
        // One for the jnz statement
        assertEquals(1, countInstances(CmpRegWithImm::class.java, result.lines()))
        // One for the jnz statement
        assertEquals(1, countInstances(Jne::class.java, result.lines()))
    }

    @Test
    fun shouldGenerateOutn() {
        val os = OutnStatement(0, 0, IDE_B)
        val result = assembleProgram(listOf(LabelledStatement("0", os)))
        assertCodeLines(result.lines(), 1, 2, 3, 2)
    }

    private fun assembleProgram(statements: List<Statement>): TargetProgram {
        val program = AstProgram(0, 0, statements).withSourcePath(Path.of("file.asmb"))
        return codeGenerator.generate(program)
    }

    companion object {
        /**
         * Asserts certain properties about the code fragments in `lines`. This method asserts that
         * the number of imported libraries and functions, the number of defined labels, and the number
         * of function calls are as specified.
         */
        private fun assertCodeLines(lines: List<Line>, libraries: Int, functions: Int, labels: Int, calls: Int) {
            assertEquals(1, countInstances(Library::class.java, lines)) // One library statement
            val numberOfImportedLibraries = lines
                .filterIsInstance<Library>()
                .sumOf { library -> library.libraries.size }
            assertEquals(libraries, numberOfImportedLibraries) // Number of imported libraries
            assertEquals(1, countInstances(Import::class.java, lines)) // One import statement
            val numberOfImportedFunctions = lines
                .filterIsInstance<Import>()
                .sumOf { import -> import.functions.size }
            assertEquals(functions, numberOfImportedFunctions) // Number of imported functions
            assertEquals(labels, countInstances(Label::class.java, lines))
            assertEquals(calls, countInstances(Call::class.java, lines))
        }

        /**
         * Returns the number of instances of `clazz` found in the list `lines`.
         */
        private fun countInstances(clazz: Class<*>, lines: List<Line>) = lines.count { obj -> clazz.isInstance(obj) }
    }
}
