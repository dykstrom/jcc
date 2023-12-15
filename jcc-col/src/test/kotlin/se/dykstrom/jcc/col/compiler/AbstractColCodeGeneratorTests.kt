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

import org.junit.jupiter.api.Assertions.assertEquals
import se.dykstrom.jcc.col.compiler.ColTests.Companion.SOURCE_PATH
import se.dykstrom.jcc.col.types.ColTypeManager
import se.dykstrom.jcc.common.ast.Program
import se.dykstrom.jcc.common.ast.Statement
import se.dykstrom.jcc.common.intermediate.IntermediateProgram
import se.dykstrom.jcc.common.intermediate.Line
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer
import se.dykstrom.jcc.common.symbols.SymbolTable
import kotlin.reflect.KClass

@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractColCodeGeneratorTests {

    val typeManager = ColTypeManager()

    val symbols = SymbolTable()

    val optimizer = DefaultAstOptimizer(typeManager, symbols)

    val codeGenerator = ColCodeGenerator(typeManager, symbols, optimizer)

    fun assembleProgram(statements: List<Statement>): IntermediateProgram =
        codeGenerator.generate(Program(0, 0, statements).withSourcePath(SOURCE_PATH))

    fun assertLibraryDependencies(dependencies: Map<String, Set<String>>, vararg expectedLibraries: String) =
        assertEquals(expectedLibraries.toSet(), dependencies.keys)

    fun assertFunctionDependencies(dependencies: Map<String, Set<String>>, vararg expectedFunctions: String) =
        assertEquals(expectedFunctions.toSet(), dependencies.values.flatten().toSet())

    fun countInstances(clazz: KClass<*>, lines: List<Line>): Int =
        lines.count { obj -> clazz.isInstance(obj) }
}
