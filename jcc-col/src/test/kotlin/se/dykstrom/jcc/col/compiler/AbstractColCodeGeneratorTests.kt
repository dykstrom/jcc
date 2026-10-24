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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import se.dykstrom.jcc.col.ast.statement.FunCallStatement
import se.dykstrom.jcc.col.type.ColTypeManager
import se.dykstrom.jcc.common.ast.AstProgram
import se.dykstrom.jcc.common.ast.Expression
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.Statement
import se.dykstrom.jcc.llvm.code.Line
import se.dykstrom.jcc.llvm.code.TargetProgram
import se.dykstrom.jcc.llvm.code.CodeGenerator
import se.dykstrom.jcc.common.functions.Function
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer
import java.nio.file.Path
import kotlin.reflect.KClass

abstract class AbstractColCodeGeneratorTests {

    val sourcePath: Path = Path.of("file.col")
    val typeManager = ColTypeManager()
    val symbols = ColSymbols()
    val optimizer = DefaultAstOptimizer(typeManager, symbols)
    val codeGenerator = ColCodeGenerator(typeManager, symbols, optimizer)
    val cg = ColCodeGenerator(typeManager, symbols, optimizer)

    fun funCall(function: Function, vararg expressions: Expression) =
        FunCallStatement(FunctionCallExpression(function.identifier, expressions.toList(), function))

    fun funCallExpr(function: Function, vararg expressions: Expression) =
        FunctionCallExpression(function.identifier, expressions.toList(), function)

    fun assembleProgram(statements: List<Statement>): TargetProgram =
        codeGenerator.generate(AstProgram(0, 0, statements).withSourcePath(sourcePath))

    fun assembleProgram(codeGenerator: CodeGenerator, statements: List<Statement>): TargetProgram =
        codeGenerator.generate(AstProgram(0, 0, statements).withSourcePath(sourcePath))

    fun assertLibraryDependencies(dependencies: Map<String, Set<String>>, vararg expectedLibraries: String) =
        assertEquals(expectedLibraries.toSet(), dependencies.keys)

    fun assertFunctionDependencies(dependencies: Map<String, Set<String>>, vararg expectedFunctions: Function) =
        assertEquals(expectedFunctions.filterIsInstance<LibraryFunction>().map { it.externalName() }.toSet(), dependencies.values.flatten().toSet())

    fun assertContains(program: TargetProgram, lines: List<String>) {
        lines.forEach { assertTrue(program.toText().contains(it), "missing line: $it") }
    }

    fun assertNotContains(program: TargetProgram, lines: List<String>) {
        lines.forEach { assertFalse(program.toText().contains(it), "unexpected line: $it") }
    }

    /** Asserts that every string in [lines] occurs in the program, in the given order. */
    fun assertInOrder(program: TargetProgram, lines: List<String>) {
        val text = program.toText()
        var index = 0
        lines.forEach {
            val found = text.indexOf(it, index)
            assertTrue(found >= 0, "missing line (or out of order): $it")
            index = found + it.length
        }
    }

    fun countInstances(clazz: KClass<*>, lines: List<Line>) =
        lines.count { obj -> clazz.isInstance(obj) }
}
