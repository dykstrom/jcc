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

package se.dykstrom.jcc.basic.compiler

import org.junit.jupiter.api.Assertions.assertEquals
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_1
import se.dykstrom.jcc.basic.optimization.BasicAstOptimizer
import se.dykstrom.jcc.common.code.Label
import se.dykstrom.jcc.common.assembly.instruction.Call
import se.dykstrom.jcc.common.assembly.macro.Import
import se.dykstrom.jcc.common.assembly.macro.Library
import se.dykstrom.jcc.common.ast.ArrayDeclaration
import se.dykstrom.jcc.common.ast.IdentifierNameExpression
import se.dykstrom.jcc.common.ast.AstProgram
import se.dykstrom.jcc.common.ast.Statement
import se.dykstrom.jcc.common.code.TargetProgram
import se.dykstrom.jcc.common.code.Line
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.optimization.AstOptimizer
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.*
import java.nio.file.Path

abstract class AbstractBasicCodeGeneratorTests {

    private val typeManager = BasicTypeManager()

    // Test with empty symbol table instead of the pre-filled one
    protected val symbols = SymbolTable()
    protected val optimizer = BasicAstOptimizer(typeManager, symbols)
    protected val codeGenerator = BasicCodeGenerator(typeManager, symbols, optimizer)

    fun assembleProgram(statements: List<Statement>): TargetProgram {
        val program = AstProgram(0, 0, statements).withSourcePath(SOURCE_PATH)
        return codeGenerator.generate(program)
    }

    /**
     * Assemble the program made up by the given list of statements, and optimize it using the given optimizer.
     */
    fun assembleProgram(statements: List<Statement>, optimizer: AstOptimizer): TargetProgram {
        val program = AstProgram(0, 0, statements).withSourcePath(SOURCE_PATH)
        return codeGenerator.generate(optimizer.program(program))
    }

    fun assertFunctionDependencies(dependencies: Map<String, Set<String>>, vararg expectedFunctions: LibraryFunction) =
        assertEquals(expectedFunctions.map { it.externalName() }.toSet(), dependencies.values.flatten().toSet())

    companion object {

        private val SOURCE_PATH = Path.of("file.bas")

        val TYPE_ARR_I64_1: Arr = Arr.from(1, I64.INSTANCE)
        val TYPE_ARR_I64_2: Arr = Arr.from(2, I64.INSTANCE)
        val TYPE_ARR_I64_3: Arr = Arr.from(3, I64.INSTANCE)
        val TYPE_ARR_F64_1: Arr = Arr.from(1, F64.INSTANCE)
        val TYPE_ARR_STR_1: Arr = Arr.from(1, Str.INSTANCE)

        val IDENT_ARR_I64_A = Identifier("a%", TYPE_ARR_I64_1)
        val IDENT_ARR_I64_B = Identifier("b%", TYPE_ARR_I64_2)
        val IDENT_ARR_I64_C = Identifier("c%", TYPE_ARR_I64_3)
        val IDENT_ARR_F64_D = Identifier("d#", TYPE_ARR_F64_1)
        val IDENT_ARR_STR_S = Identifier("s$", TYPE_ARR_STR_1)
        val IDENT_ARR_I64_X = Identifier("x", TYPE_ARR_I64_1)

        val INE_ARR_I64_X = IdentifierNameExpression(0, 0, IDENT_ARR_I64_X)

        val DECL_ARR_I64_X = ArrayDeclaration(0, 0, IDENT_ARR_I64_X.name(), TYPE_ARR_I64_1, listOf(IL_1))

        fun assertCodeLines(lines: List<Line>, libraries: Int, functions: Int, labels: Int, calls: Int) {
            assertEquals(1, countInstances(Library::class.java, lines)) // One library statement
            val numberOfImportedLibraries = lines
                .filterIsInstance<Library>()
                .sumOf { library -> library.libraries.size }
            assertEquals(libraries, numberOfImportedLibraries) // Number of imported libraries
            val numberOfImportedFunctions = lines
                .filterIsInstance<Import>()
                .sumOf { import -> import.functions.size }
            assertEquals(functions, numberOfImportedFunctions) // Number of imported functions
            assertEquals(labels, countInstances(Label::class.java, lines))
            assertEquals(calls, countInstances(Call::class.java, lines))
        }

        fun countInstances(clazz: Class<*>, lines: List<Line>): Int = lines.count { obj -> clazz.isInstance(obj) }
    }
}
