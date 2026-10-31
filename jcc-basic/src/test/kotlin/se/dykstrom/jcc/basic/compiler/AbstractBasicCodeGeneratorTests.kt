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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import se.dykstrom.jcc.basic.optimization.BasicAstOptimizer
import se.dykstrom.jcc.basic.type.BasicTypeManager
import se.dykstrom.jcc.common.ast.AstProgram
import se.dykstrom.jcc.common.ast.Statement
import se.dykstrom.jcc.common.optimization.AstOptimizer
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.llvm.code.CodeGenerator
import se.dykstrom.jcc.llvm.code.TargetProgram
import java.nio.file.Path

abstract class AbstractBasicCodeGeneratorTests {

    protected val typeManager = BasicTypeManager()
    // Test with empty symbol table instead of the pre-filled one
    protected val symbols = SymbolTable()
    protected val optimizer = BasicAstOptimizer(typeManager, symbols)
    protected val cg = BasicCodeGenerator(typeManager, symbols, optimizer)

    fun assembleProgram(codeGenerator: CodeGenerator, statements: List<Statement>): TargetProgram =
        codeGenerator.generate(AstProgram(0, 0, statements).withSourcePath(SOURCE_PATH))

    /**
     * Assemble the program made up by the given list of statements using the given code
     * generator, after optimizing it using the given optimizer.
     */
    fun assembleProgram(codeGenerator: CodeGenerator, statements: List<Statement>, optimizer: AstOptimizer): TargetProgram =
        codeGenerator.generate(optimizer.program(AstProgram(0, 0, statements).withSourcePath(SOURCE_PATH)))

    fun assertContains(program: TargetProgram, lines: List<String>) {
        lines.forEach { assertTrue(program.toText().contains(it), "missing line: $it") }
    }

    fun assertNotContains(program: TargetProgram, lines: List<String>) {
        lines.forEach { assertFalse(program.toText().contains(it), "unexpected line: $it") }
    }

    companion object {
        private val SOURCE_PATH = Path.of("file.bas")
    }
}
