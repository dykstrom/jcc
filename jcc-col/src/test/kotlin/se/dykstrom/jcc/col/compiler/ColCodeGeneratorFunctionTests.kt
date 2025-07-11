/*
 * Copyright (C) 2023 Johan Dykstrom
 *
 * This program is ceil software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Ceil Software Foundation, either version 3 of the License, or
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
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ast.statement.FunCallStatement
import se.dykstrom.jcc.col.ast.statement.ImportStatement
import se.dykstrom.jcc.col.compiler.ColFunctions.BF_PRINTLN_F64
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FL_1_0
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_ABS
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FUN_SUM0
import se.dykstrom.jcc.common.assembly.instruction.Call
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.functions.LibcBuiltIns.*

class ColCodeGeneratorFunctionTests : AbstractColCodeGeneratorTests() {

    @Test
    fun shouldGeneratePrintlnFunctionCall() {
        // Given
        val fce = FunctionCallExpression(0, 0, LF_CEIL_F64.identifier, listOf(FL_1_0))
        val ps = funCall(BF_PRINTLN_F64, fce)

        // When
        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Then
        assertLibraryDependencies(codeGenerator.dependencies(), "msvcrt.dll")
        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT.externalName(), FUN_PRINTF_STR_VAR.externalName(), LF_CEIL_F64.externalName())
        // ceil, printf and exit
        assertEquals(3, countInstances(Call::class, lines))
    }

    @Test
    fun shouldGenerateStandAloneFunctionCall() {
        // Given
        val fce = FunctionCallExpression(0, 0, LF_CEIL_F64.identifier, listOf(FL_1_0))
        val fcs = FunCallStatement(0, 0, fce)

        // When
        val result = assembleProgram(listOf(fcs))
        val lines = result.lines()

        // Then
        assertLibraryDependencies(codeGenerator.dependencies(), "msvcrt.dll")
        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT.externalName(), LF_CEIL_F64.externalName())
        // ceil and exit
        assertEquals(2, countInstances(Call::class, lines))
    }

    @Test
    fun shouldGenerateImportFunction() {
        // Given
        val statement = ImportStatement(0, 0, FUN_SUM0)

        // When
        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // Then
        assertLibraryDependencies(codeGenerator.dependencies(), "lib.dll", "msvcrt.dll")
        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT.name, FUN_SUM0.name)
        // exit
        assertEquals(1, countInstances(Call::class, lines))
    }

    @Test
    fun shouldGenerateImportFunctionFromMsvcrt() {
        // Given
        val statement = ImportStatement(0, 0, FUN_ABS)

        // When
        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // Then
        assertLibraryDependencies(codeGenerator.dependencies(), "msvcrt.dll")
        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT.name, "_abs64")
        // exit
        assertEquals(1, countInstances(Call::class, lines))
    }
}
