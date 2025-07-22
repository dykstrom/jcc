/*
 * Copyright (C) 2024 Johan Dykstrom
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
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.compiler.ColSymbols.*
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FL_1_0
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_17
import se.dykstrom.jcc.common.assembly.instruction.MoveWithSignExtend
import se.dykstrom.jcc.common.assembly.instruction.floating.ConvertIntRegToFloatReg
import se.dykstrom.jcc.common.assembly.instruction.floating.TruncateFloatRegToIntReg
import se.dykstrom.jcc.common.ast.CastToF64Expression
import se.dykstrom.jcc.common.ast.CastToI32Expression
import se.dykstrom.jcc.common.ast.CastToI64Expression

class ColCodeGeneratorCastTests : AbstractColCodeGeneratorTests() {

    @Test
    fun shouldGenerateCastToF64() {
        // Given
        val ce = CastToF64Expression(0, 0, IL_17)
        val ps = funCall(BF_PRINTLN_F64, ce)

        // When
        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Then
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class, lines))
    }

    @Test
    fun shouldGenerateCastToI32() {
        // Given
        val ce = CastToI32Expression(0, 0, IL_17)
        val ps = funCall(BF_PRINTLN_I32, ce)

        // When
        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Then
        assertEquals(1, countInstances(MoveWithSignExtend::class, lines))
    }

    @Test
    fun shouldGenerateCastToI64() {
        // Given
        val ce = CastToI64Expression(0, 0, FL_1_0)
        val ps = funCall(BF_PRINTLN_I64, ce)

        // When
        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Then
        assertEquals(1, countInstances(TruncateFloatRegToIntReg::class, lines))
    }
}
