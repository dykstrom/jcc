/*
 * Copyright (C) 2019 Johan Dykstrom
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

import org.junit.Before
import org.junit.Test
import se.dykstrom.jcc.common.assembly.instruction.*
import se.dykstrom.jcc.common.assembly.other.DataDefinition
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.AssignStatement
import se.dykstrom.jcc.common.ast.SubExpression
import se.dykstrom.jcc.common.compiler.DefaultAstOptimizer
import se.dykstrom.jcc.common.utils.OptimizationOptions
import kotlin.test.assertEquals

/**
 * Tests features related to optimization in code generation.
 *
 * @author Johan Dykstrom
 */
class BasicCodeGeneratorOptimizationTests : AbstractBasicCodeGeneratorTest() {

    @Before
    fun init() {
        OptimizationOptions.INSTANCE.level = 1
    }

    /**
     * After replacing the assign statement and add expression with an inc statement,
     * there should be one instance of IncMem to increment the variable.
     */
    @Test
    fun shouldReplaceAddOneWithInc() {
        val addExpression = AddExpression(0, 0, IDE_I64_A, IL_1)
        val assignStatement = AssignStatement(0, 0, IDENT_I64_A, addExpression)

        val result = assembleProgram(listOf(assignStatement), OPTIMIZER)
        val codes = result.codes()

        assertEquals(1, countInstances(IncMem::class.java, codes))
    }

    /**
     * After replacing the assign statement and sub expression with a dec statement,
     * there should be one instance of DecMem to decrement the variable.
     */
    @Test
    fun shouldReplaceDecOneWithDec() {
        val subExpression = SubExpression(0, 0, IDE_I64_A, IL_1)
        val assignStatement = AssignStatement(0, 0, IDENT_I64_A, subExpression)

        val result = assembleProgram(listOf(assignStatement), OPTIMIZER)
        val codes = result.codes()

        assertEquals(1, countInstances(DecMem::class.java, codes))
    }

    /**
     * After replacing the assign statement and add expression with an add-assign statement,
     * there should be one instance of operation AddImmToMem.
     */
    @Test
    fun shouldReplaceAddTwoWithAddAssign() {
        val addExpression = AddExpression(0, 0, IDE_I64_A, IL_2)
        val assignStatement = AssignStatement(0, 0, IDENT_I64_A, addExpression)

        val result = assembleProgram(listOf(assignStatement), OPTIMIZER)
        val codes = result.codes()

        assertEquals(1, countInstances(AddImmToMem::class.java, codes))
    }

    /**
     * After replacing the assign statement and sub expression with a sub-assign statement,
     * there should be one instance of operation SubImmFromMem.
     */
    @Test
    fun shouldReplaceSubTwoWithSubAssign() {
        val subExpression = SubExpression(0, 0, IDE_I64_A, IL_2)
        val assignStatement = AssignStatement(0, 0, IDENT_I64_A, subExpression)

        val result = assembleProgram(listOf(assignStatement), OPTIMIZER)
        val codes = result.codes()

        assertEquals(1, countInstances(SubImmFromMem::class.java, codes))
    }

    /**
     * After replacing the add expression with an integer literal,
     * there should be one instance of operation MoveImmToReg where
     * the literal value equals the result of the inlined addition.
     */
    @Test
    fun shouldReplaceAddIntegerLiteralsWithOneLiteral() {
        val addExpression = AddExpression(0, 0, IL_1, IL_2)
        val assignStatement = AssignStatement(0, 0, IDENT_I64_A, addExpression)

        val result = assembleProgram(listOf(assignStatement), OPTIMIZER)
        val codes = result.codes()

        val count = codes.filterIsInstance(MoveImmToReg::class.java)
                .filter { it.immediate == "3" }
                .count()
        assertEquals(1, count)
    }

    /**
     * After replacing the add expression with a string literal,
     * there should be one data definition where the value equals
     * the result of the inlined addition.
     */
    @Test
    fun shouldReplaceAddStringLiteralsWithOneLiteral() {
        val addExpression = AddExpression(0, 0, SL_ONE, SL_TWO)
        val assignStatement = AssignStatement(0, 0, IDENT_STR_B, addExpression)

        val result = assembleProgram(listOf(assignStatement), OPTIMIZER)
        val codes = result.codes()

        val count = codes.filterIsInstance(DataDefinition::class.java)
                .filter { it.value.contains("OneTwo") }
                .count()
        assertEquals(1, count)
    }

    companion object {
        private val OPTIMIZER = DefaultAstOptimizer()
    }
}
