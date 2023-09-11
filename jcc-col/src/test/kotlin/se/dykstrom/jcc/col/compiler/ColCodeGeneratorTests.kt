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

import org.junit.Test
import se.dykstrom.jcc.col.ast.PrintlnStatement
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_17
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_18
import se.dykstrom.jcc.common.assembly.base.Label
import se.dykstrom.jcc.common.assembly.instruction.Add
import se.dykstrom.jcc.common.assembly.instruction.Call
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_EXIT
import se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_PRINTF
import kotlin.test.assertEquals

class ColCodeGeneratorTests : AbstractColCodeGeneratorTests() {

    @Test
    fun shouldGenerateEmptyProgram() {
        // When
        val result = assembleProgram(listOf())
        val lines = result.lines()

        // Then
        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT.name)
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
        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT.name, FUN_PRINTF.name)
        // 17 + 18, and 2* clean up shadow space
        assertEquals(3, countInstances(Add::class, lines))
        // printf and exit
        assertEquals(2, countInstances(Call::class, lines))
    }
}
