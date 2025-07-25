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
import org.junit.jupiter.api.Assertions.assertTrue
import se.dykstrom.jcc.col.ast.statement.FunCallStatement
import se.dykstrom.jcc.col.compiler.ColTests.Companion.SOURCE_PATH
import se.dykstrom.jcc.col.types.ColTypeManager
import se.dykstrom.jcc.common.assembly.instruction.CallDirect
import se.dykstrom.jcc.common.ast.AstProgram
import se.dykstrom.jcc.common.ast.Expression
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.Statement
import se.dykstrom.jcc.common.code.Line
import se.dykstrom.jcc.common.code.TargetProgram
import se.dykstrom.jcc.common.compiler.CodeGenerator
import se.dykstrom.jcc.common.functions.Function
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer
import kotlin.reflect.KClass

abstract class AbstractColCodeGeneratorTests {

    val typeManager = ColTypeManager()
    val symbols = ColSymbols()
    val optimizer = DefaultAstOptimizer(typeManager, symbols)
    val codeGenerator = ColCodeGenerator(typeManager, symbols, optimizer)

    fun funCall(function: Function, vararg expressions: Expression) =
        FunCallStatement(FunctionCallExpression(function.identifier, expressions.toList()))

    fun assembleProgram(statements: List<Statement>): TargetProgram =
        codeGenerator.generate(AstProgram(0, 0, statements).withSourcePath(SOURCE_PATH))

    fun assembleProgram(codeGenerator: CodeGenerator, statements: List<Statement>): TargetProgram =
        codeGenerator.generate(AstProgram(0, 0, statements).withSourcePath(SOURCE_PATH))

    fun assertLibraryDependencies(dependencies: Map<String, Set<String>>, vararg expectedLibraries: String) =
        assertEquals(expectedLibraries.toSet(), dependencies.keys)

    fun assertFunctionDependencies(dependencies: Map<String, Set<String>>, vararg expectedFunctions: LibraryFunction) =
        assertEquals(expectedFunctions.map { it.externalName() }.toSet(), dependencies.values.flatten().toSet())

    fun assertContains(program: TargetProgram, lines: List<String>) {
        lines.forEach { assertTrue(program.toText().contains(it), "missing line: $it") }
    }

    fun countInstances(clazz: KClass<*>, lines: List<Line>) =
        lines.count { obj -> clazz.isInstance(obj) }

    fun hasDirectCallTo(lines: List<Line>, mappedName: String) =
        lines.filterIsInstance<CallDirect>().any { it.target == "_$mappedName" }
}
