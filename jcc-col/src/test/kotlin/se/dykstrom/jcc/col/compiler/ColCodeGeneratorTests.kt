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
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ast.PrintlnStatement
import se.dykstrom.jcc.col.compiler.ColTests.Companion.FL_1_0
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_17
import se.dykstrom.jcc.col.compiler.ColTests.Companion.IL_18
import se.dykstrom.jcc.common.assembly.instruction.*
import se.dykstrom.jcc.common.assembly.instruction.floating.MulFloat
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.ast.BooleanLiteral.FALSE
import se.dykstrom.jcc.common.ast.BooleanLiteral.TRUE
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import se.dykstrom.jcc.common.code.Label
import se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_EXIT
import se.dykstrom.jcc.common.functions.BuiltInFunctions.FUN_PRINTF

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
    fun shouldGeneratePrintlnAddExpression() {
        // Given
        val ae = AddExpression(0, 0, IL_17, IL_18)
        val ps = PrintlnStatement(0, 0, ae)

        // When
        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Then
        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT.name, FUN_PRINTF.name)
        // 17 + 18, and 2 * clean up shadow space
        assertEquals(3, countInstances(Add::class, lines))
        // printf and exit
        assertEquals(2, countInstances(Call::class, lines))
    }

    @Test
    fun shouldGeneratePrintlnNegatedMulExpression() {
        // Given
        val ae = MulExpression(0, 0, IL_17, IL_18)
        val ne = NegateExpression(0, 0, ae)
        val ps = PrintlnStatement(0, 0, ne)

        // When
        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Then
        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT.name, FUN_PRINTF.name)
        // 2 * clean up shadow space
        assertEquals(2, countInstances(Add::class, lines))
        // 17 * 18
        assertEquals(1, countInstances(IMul::class, lines))
        // negate
        assertEquals(1, countInstances(NegReg::class, lines))
        // printf and exit
        assertEquals(2, countInstances(Call::class, lines))
    }

    @Test
    fun shouldGeneratePrintlnMulFloatsExpression() {
        // Given
        val ae = MulExpression(0, 0, FL_1_0, FL_1_0)
        val ps = PrintlnStatement(0, 0, ae)

        // When
        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Then
        assertFunctionDependencies(codeGenerator.dependencies(), FUN_EXIT.name, FUN_PRINTF.name)
        // 2 * clean up shadow space
        assertEquals(2, countInstances(Add::class, lines))
        // 1.0 * 1.0
        assertEquals(1, countInstances(MulFloat::class, lines))
        // printf and exit
        assertEquals(2, countInstances(Call::class, lines))
    }

    @Test
    fun shouldGeneratePrintlnBitwiseNot() {
        // Given
        val ne = NotExpression(0, 0, IL_17)
        val ps = PrintlnStatement(0, 0, ne)

        // When
        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Then
        assertEquals(1, countInstances(NotReg::class, lines))
    }

    @Test
    fun shouldGeneratePrintlnBitwiseXor() {
        // Given
        val xe = XorExpression(0, 0, IL_17, ZERO)
        val ps = PrintlnStatement(0, 0, xe)

        // When
        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Then
        assertEquals(1, countInstances(XorRegWithReg::class, lines))
    }

    @Test
    fun shouldGeneratePrintlnLogicalNot() {
        // Given
        val ne = LogicalNotExpression(0, 0, FALSE)
        val ps = PrintlnStatement(0, 0, ne)

        // When
        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Then
        assertEquals(1, countInstances(NotReg::class, lines))
    }

    @Test
    fun shouldGeneratePrintlnLogicalXor() {
        // Given
        val xe = LogicalXorExpression(0, 0, TRUE, TRUE)
        val ps = PrintlnStatement(0, 0, xe)

        // When
        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Then
        assertEquals(1, countInstances(XorRegWithReg::class, lines))
    }

    @Test
    fun shouldGeneratePrintlnLogicalAnd() {
        // Given
        val ae = LogicalAndExpression(0, 0, TRUE, TRUE)
        val ps = PrintlnStatement(0, 0, ae)

        // When
        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Then
        assertEquals(1, countInstances(AndRegWithReg::class, lines))
        assertEquals(1, countInstances(CmpRegWithImm::class, lines))
        assertEquals(1, countInstances(Je::class, lines))
    }

    @Test
    fun shouldGeneratePrintlnLogicalOr() {
        // Given
        val oe = LogicalOrExpression(0, 0, TRUE, TRUE)
        val ps = PrintlnStatement(0, 0, oe)

        // When
        val result = assembleProgram(listOf(ps))
        val lines = result.lines()

        // Then
        assertEquals(1, countInstances(OrRegWithReg::class, lines))
        assertEquals(1, countInstances(CmpRegWithImm::class, lines))
        assertEquals(1, countInstances(Jne::class, lines))
    }
}
