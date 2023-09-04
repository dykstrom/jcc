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

import org.junit.Before
import org.junit.Test
import se.dykstrom.jcc.col.ast.PrintlnStatement
import se.dykstrom.jcc.common.assembly.base.Label
import se.dykstrom.jcc.common.assembly.instruction.Add
import se.dykstrom.jcc.common.assembly.instruction.Call
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.compiler.DefaultTypeManager
import se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_EXIT
import se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_PRINTF
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.intermediate.Line
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.I64
import java.nio.file.Path
import kotlin.reflect.KClass
import kotlin.test.assertEquals

class ColCodeGeneratorTests {

    private val typeManager = DefaultTypeManager()

    private val symbols = SymbolTable()

    private val optimizer = DefaultAstOptimizer(typeManager, symbols)

    private val codeGenerator = ColCodeGenerator(typeManager, symbols, optimizer)

    @Before
    fun setUp() {
        symbols.addFunction(FUN_SUM0)
    }

    @Test
    fun shouldGenerateEmptyProgram() {
        // When
        val result = assembleProgram(listOf())
        val lines = result.lines()

        // Then
        assertDependencies(codeGenerator.dependencies(), FUN_EXIT.name)
        assertEquals(1, countInstances(Label::class, lines))
        assertEquals(1, countInstances(Call::class, lines))
    }

    @Test
    fun shouldGeneratePrintlnExpression() {
        // Given
        val ae = AddExpression(0, 0, IL_17, IL_18)
        val ps = PrintlnStatement(0, 0, ae)

        // When
        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Then
        assertDependencies(codeGenerator.dependencies(), FUN_EXIT.name, FUN_PRINTF.name)
        // 17 + 18, and 2* clean up shadow space
        assertEquals(3, countInstances(Add::class, lines))
        // printf and exit
        assertEquals(2, countInstances(Call::class, lines))
    }

    @Test
    fun shouldGeneratePrintlnFunctionCall() {
        // Given
        val fce = FunctionCallExpression(0, 0, FUN_SUM0.identifier, listOf())
        val ps = PrintlnStatement(0, 0, fce)

        // When
        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Then
        assertDependencies(codeGenerator.dependencies(), FUN_EXIT.name, FUN_PRINTF.name, FUN_SUM0.name)
        // printf and exit
        assertEquals(3, countInstances(Call::class, lines))
    }

    private fun assembleProgram(statements: List<Statement>) =
        codeGenerator.generate(Program(0, 0, statements).withSourcePath(SOURCE_PATH))

    private fun assertDependencies(dependencies: Map<String, Set<String>>, vararg expectedFunctions: String) {
        assertEquals(expectedFunctions.toSet(), dependencies.values.flatten().toSet())
    }

    private fun countInstances(clazz: KClass<*>, lines: List<Line>): Int =
        lines.count { obj -> clazz.isInstance(obj) }

    companion object {
        private val SOURCE_PATH = Path.of("file.col")

        private val FUN_SUM0 = LibraryFunction("sum", listOf(), I64.INSTANCE, "foolib", ExternalFunction("sum"))

        private val IL_17 = IntegerLiteral(0, 0, 17)
        private val IL_18 = IntegerLiteral(0, 0, 18)
    }
}
