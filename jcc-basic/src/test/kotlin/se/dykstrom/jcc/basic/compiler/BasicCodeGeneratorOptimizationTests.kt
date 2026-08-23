/*
 * Copyright (C) 2026 Johan Dykstrom
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

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_I64_H
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_1
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_3
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_4
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_I64_A
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.AssignStatement
import se.dykstrom.jcc.common.ast.IDivExpression
import se.dykstrom.jcc.common.ast.MulExpression
import se.dykstrom.jcc.common.ast.SubExpression
import se.dykstrom.jcc.common.utils.OptimizationOptions

/**
 * Tests features related to optimization in the LLVM code generator. The statements
 * assign to variables that have not been seen before, so these tests also verify
 * that the optimized statements register their variables as globals.
 *
 * @author Johan Dykstrom
 */
internal class BasicCodeGeneratorOptimizationTests : AbstractBasicCodeGeneratorTests() {

    private val cg = BasicCodeGenerator(typeManager, symbols, optimizer)

    @BeforeEach
    fun init() {
        OptimizationOptions.INSTANCE.level = 1
    }

    @AfterEach
    fun tearDown() {
        OptimizationOptions.INSTANCE.level = 0
    }

    @Test
    fun shouldReplaceAddOneWithInc() {
        val addExpression = AddExpression(0, 0, IDE_I64_A, IL_1)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, addExpression)

        val result = assembleProgram(cg, listOf(assignStatement), optimizer)

        assertContains(result, listOf("%1 = add i64 %0, 1", "store i64 %1"))
    }

    @Test
    fun shouldReplaceSubOneWithDec() {
        val subExpression = SubExpression(0, 0, IDE_I64_A, IL_1)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, subExpression)

        val result = assembleProgram(cg, listOf(assignStatement), optimizer)

        assertContains(result, listOf("%1 = sub i64 %0, 1", "store i64 %1"))
    }

    @Test
    fun shouldReplaceAddThreeWithAddAssign() {
        val addExpression = AddExpression(0, 0, IDE_I64_A, IL_3)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, addExpression)

        val result = assembleProgram(cg, listOf(assignStatement), optimizer)

        assertContains(result, listOf("%1 = add i64 %0, 3", "store i64 %1"))
    }

    @Test
    fun shouldReplaceMulThreeWithMulAssign() {
        val mulExpression = MulExpression(0, 0, IDE_I64_A, IL_3)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, mulExpression)

        val result = assembleProgram(cg, listOf(assignStatement), optimizer)

        assertContains(result, listOf("%1 = mul i64 %0, 3", "store i64 %1"))
    }

    @Test
    fun shouldReplaceIDivThreeWithIDivAssign() {
        val iDivExpression = IDivExpression(0, 0, IDE_I64_A, IL_3)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, iDivExpression)

        val result = assembleProgram(cg, listOf(assignStatement), optimizer)

        assertContains(result, listOf("%1 = sdiv i64 %0, 3", "store i64 %1"))
    }

    @Test
    fun shouldReplaceMulWithPowerOfTwoWithShift() {
        val mulExpression = MulExpression(0, 0, IDE_I64_H, IL_4)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, mulExpression)

        val result = assembleProgram(cg, listOf(assignStatement), optimizer)

        assertContains(result, listOf("%1 = shl i64 %0, 2", "store i64 %1"))
    }
}
