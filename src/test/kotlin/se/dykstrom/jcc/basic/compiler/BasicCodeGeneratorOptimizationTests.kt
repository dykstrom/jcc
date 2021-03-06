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
import se.dykstrom.jcc.basic.functions.BasicBuiltInFunctions.FUN_SGN
import se.dykstrom.jcc.common.assembly.instruction.*
import se.dykstrom.jcc.common.assembly.instruction.floating.MoveFloatRegToMem
import se.dykstrom.jcc.common.assembly.instruction.floating.MoveMemToFloatReg
import se.dykstrom.jcc.common.assembly.other.DataDefinition
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer
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

        defineFunction(FUN_SGN)
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

    /**
     * After replacing the sub expression with an integer literal,
     * there should be one instance of operation MoveImmToReg where
     * the literal value equals the result of the inlined subtraction.
     */
    @Test
    fun shouldReplaceSubIntegerLiteralsWithOneLiteral() {
        val subExpression = SubExpression(0, 0, IL_1, IL_2)
        val assignStatement = AssignStatement(0, 0, IDENT_I64_A, subExpression)

        val result = assembleProgram(listOf(assignStatement), OPTIMIZER)
        val codes = result.codes()

        val count = codes.filterIsInstance(MoveImmToReg::class.java)
                .filter { it.immediate == "-1" }
                .count()
        assertEquals(1, count)
    }

    /**
     * After replacing the multiplication with a power of two, there should be one
     * instance of SalRegWithCL, that represents the shift operation that gives the
     * same result as the multiplication.
     */
    @Test
    fun shouldReplaceMulWithPowerOfTwoWithShift() {
        val mulExpression = MulExpression(0, 0, IDE_I64_H, IL_2)
        val assignStatement = AssignStatement(0, 0, IDENT_I64_A, mulExpression)

        val result = assembleProgram(listOf(assignStatement), OPTIMIZER)
        val codes = result.codes()

        val count = codes.filterIsInstance(SalRegWithCL::class.java).count()
        assertEquals(1, count)
    }

    /**
     * After replacing the mul expression with zero, there should be one instance
     * of operation MoveImmToReg where the literal value equals the result of the
     * inlined addition. There should be no multiplication operations.
     */
    @Test
    fun shouldReplaceMulWithZeroWithJustZero() {
        val mulExpression = MulExpression(0, 0, IDE_I64_H, IL_0)
        val assignStatement = AssignStatement(0, 0, IDENT_I64_A, mulExpression)

        val result = assembleProgram(listOf(assignStatement), OPTIMIZER)
        val codes = result.codes()

        // One for the optimized multiplication, and one for the call to exit
        assertEquals(2, codes.filterIsInstance(MoveImmToReg::class.java).filter { it.immediate == "0" }.count())
        assertEquals(0, codes.filterIsInstance(IMulMemWithReg::class.java).count())
    }

    /**
     * If there is a function call in the expression, it should not be replaced with zero,
     * since the function call may have side effects.
     */
    @Test
    fun shouldNotReplaceMulFunctionCallWithZeroWithJustZero() {
        val functionCall = FunctionCallExpression(0, 0, FUN_SGN.identifier, listOf(IL_1))
        val mulExpression = MulExpression(0, 0, functionCall, IL_0)
        val assignStatement = AssignStatement(0, 0, IDENT_I64_A, mulExpression)

        val result = assembleProgram(listOf(assignStatement), OPTIMIZER)
        val codes = result.codes()

        // One for the optimized multiplication, and one for the call to exit
        assertEquals(1, codes.filterIsInstance(CallIndirect::class.java).filter { it.target.contains(FUN_SGN.mappedName) }.count())
        assertEquals(1, codes.filterIsInstance(IMulRegWithReg::class.java).count())
    }

    /**
     * After replacing the idiv expression with an integer literal,
     * there should be one instance of operation MoveImmToReg where
     * the literal value equals the result of the inlined division.
     */
    @Test
    fun shouldReplaceIDivIntegerLiteralsWithOneLiteral() {
        val iDivExpression = IDivExpression(0, 0, IL_4, IL_2)
        val assignStatement = AssignStatement(0, 0, IDENT_I64_A, iDivExpression)

        val result = assembleProgram(listOf(assignStatement), OPTIMIZER)
        val codes = result.codes()

        val count = codes.filterIsInstance(MoveImmToReg::class.java)
                .filter { it.immediate == "2" }
                .count()
        assertEquals(1, count)
    }

    /**
     * After replacing the div expression with a float literal,
     * there should be one instance of operation MoveMemToFloatReg where
     * the destination is an XMM register, and one MoveFloatRegToMem where
     * the source is an XMM register. These operations are used to
     * transfer the float from a constant to a variable.
     */
    @Test
    fun shouldReplaceDivFloatLiteralsWithOneLiteral() {
        val divExpression = DivExpression(0, 0, FL_3_14, IL_1)
        val assignStatement = AssignStatement(0, 0, IDENT_F64_F, divExpression)

        val result = assembleProgram(listOf(assignStatement), OPTIMIZER)
        val codes = result.codes()

        assertEquals(1, codes.filterIsInstance(MoveMemToFloatReg::class.java).filter { it.destination.startsWith("xmm") }.count())
        assertEquals(1, codes.filterIsInstance(MoveFloatRegToMem::class.java).filter { it.source.startsWith("xmm") }.count())
    }

    companion object {
        private val OPTIMIZER = DefaultAstOptimizer(BasicTypeManager())
    }
}
