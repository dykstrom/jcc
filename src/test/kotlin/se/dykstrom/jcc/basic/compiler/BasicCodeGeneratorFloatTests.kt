/*
 * Copyright (C) 2018 Johan Dykstrom
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

import org.junit.Assert.assertEquals
import org.junit.Test
import se.dykstrom.jcc.basic.ast.PrintStatement
import se.dykstrom.jcc.common.assembly.base.Code
import se.dykstrom.jcc.common.assembly.instruction.*
import se.dykstrom.jcc.common.assembly.instruction.floating.*
import se.dykstrom.jcc.common.ast.*

class BasicCodeGeneratorFloatTests : AbstractBasicCodeGeneratorTest() {

    @Test
    fun shouldAssignFloatLiteral() {
        val assignStatement = AssignStatement(0, 0, IDENT_F64_F, FL_3_14)

        val result = assembleProgram(listOf(assignStatement))

        val codes = result.codes()
        // Exit code
        assertEquals(1, countInstances(MoveImmToReg::class.java, codes))
        // Evaluating the float literal
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, codes))
        // Storing the evaluated float literal
        assertEquals(1, countInstances(MoveFloatRegToMem::class.java, codes))
    }

    @Test
    fun shouldAssignAddFloatFloatExpression() {
        val addExpression = AddExpression(0, 0, FL_3_14, FL_17_E4)
        val assignStatement = AssignStatement(0, 0, IDENT_F64_F, addExpression)

        val result = assembleProgram(listOf(assignStatement))

        val codes = result.codes()
        assertEquals(1, countInstances(AddFloatRegToFloatReg::class.java, codes))
        assertAssignmentToF(codes)
    }

    @Test
    fun shouldAssignAddFloatIntegerExpression() {
        val addExpression = AddExpression(0, 0, FL_3_14, IL_3)
        val assignStatement = AssignStatement(0, 0, IDENT_F64_F, addExpression)

        val result = assembleProgram(listOf(assignStatement))

        val codes = result.codes()
        assertEquals(1, countInstances(AddFloatRegToFloatReg::class.java, codes))
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, codes))
        assertAssignmentToF(codes)
    }

    @Test
    fun shouldAssignAddIntegerFloatExpression() {
        val addExpression = AddExpression(0, 0, IL_3, FL_3_14)
        val assignStatement = AssignStatement(0, 0, IDENT_F64_F, addExpression)

        val result = assembleProgram(listOf(assignStatement))

        val codes = result.codes()
        assertEquals(1, countInstances(AddFloatRegToFloatReg::class.java, codes))
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, codes))
        assertAssignmentToF(codes)
    }

    @Test
    fun shouldAssignComplexAddExpression() {
        val addExpression1 = AddExpression(0, 0, IL_3, FL_3_14)
        val addExpression2 = AddExpression(0, 0, IL_2, FL_17_E4)
        val addExpression3 = AddExpression(0, 0, addExpression1, addExpression2)
        val assignStatement = AssignStatement(0, 0, IDENT_F64_F, addExpression3)

        val result = assembleProgram(listOf(assignStatement))

        val codes = result.codes()
        assertEquals(3, countInstances(AddFloatRegToFloatReg::class.java, codes))
        assertEquals(2, countInstances(ConvertIntRegToFloatReg::class.java, codes))
        assertAssignmentToF(codes)
    }

    @Test
    fun shouldAssignSubFloatFloatExpression() {
        val subExpression = SubExpression(0, 0, FL_3_14, FL_17_E4)
        val assignStatement = AssignStatement(0, 0, IDENT_F64_F, subExpression)

        val result = assembleProgram(listOf(assignStatement))

        val codes = result.codes()
        assertEquals(1, countInstances(SubFloatRegFromFloatReg::class.java, codes))
        assertAssignmentToF(codes)
    }

    @Test
    fun shouldAssignSubFloatIntegerExpression() {
        val subExpression = SubExpression(0, 0, FL_3_14, IL_1)
        val assignStatement = AssignStatement(0, 0, IDENT_F64_F, subExpression)

        val result = assembleProgram(listOf(assignStatement))

        val codes = result.codes()
        assertEquals(1, countInstances(SubFloatRegFromFloatReg::class.java, codes))
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, codes))
        assertAssignmentToF(codes)
    }

    @Test
    fun shouldAssignSubIntegerFloatExpression() {
        val subExpression = SubExpression(0, 0, IL_4, FL_17_E4)
        val assignStatement = AssignStatement(0, 0, IDENT_F64_F, subExpression)

        val result = assembleProgram(listOf(assignStatement))

        val codes = result.codes()
        assertEquals(1, countInstances(SubFloatRegFromFloatReg::class.java, codes))
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, codes))
        assertAssignmentToF(codes)
    }

    @Test
    fun shouldAssignMulFloatFloatExpression() {
        val mulExpression = MulExpression(0, 0, FL_3_14, FL_17_E4)
        val assignStatement = AssignStatement(0, 0, IDENT_F64_F, mulExpression)

        val result = assembleProgram(listOf(assignStatement))

        val codes = result.codes()
        assertEquals(1, countInstances(MulFloatRegWithFloatReg::class.java, codes))
        assertAssignmentToF(codes)
    }

    @Test
    fun shouldAssignDivFloatFloatExpression() {
        val divExpression = DivExpression(0, 0, FL_3_14, FL_17_E4)
        val assignStatement = AssignStatement(0, 0, IDENT_F64_F, divExpression)

        val result = assembleProgram(listOf(assignStatement))

        val codes = result.codes()
        assertEquals(1, countInstances(DivFloatRegWithFloatReg::class.java, codes))
        assertAssignmentToF(codes)
    }

    @Test
    fun shouldAssignDivFloatIntegerExpression() {
        val divExpression = DivExpression(0, 0, FL_3_14, IL_3)
        val assignStatement = AssignStatement(0, 0, IDENT_F64_F, divExpression)

        val result = assembleProgram(listOf(assignStatement))

        val codes = result.codes()
        assertEquals(1, countInstances(DivFloatRegWithFloatReg::class.java, codes))
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, codes))
        assertAssignmentToF(codes)
    }

    @Test
    fun shouldAssignDivIntegerFloatExpression() {
        val divExpression = DivExpression(0, 0, IL_2, FL_3_14)
        val assignStatement = AssignStatement(0, 0, IDENT_F64_F, divExpression)

        val result = assembleProgram(listOf(assignStatement))

        val codes = result.codes()
        assertEquals(1, countInstances(DivFloatRegWithFloatReg::class.java, codes))
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, codes))
        assertAssignmentToF(codes)
    }

    @Test
    fun shouldAssignDivIntegerIntegerExpression() {
        val divExpression = DivExpression(0, 0, IL_2, IL_4)
        val assignStatement = AssignStatement(0, 0, IDENT_F64_F, divExpression)

        val result = assembleProgram(listOf(assignStatement))

        val codes = result.codes()
        assertEquals(1, countInstances(DivFloatRegWithFloatReg::class.java, codes))
        assertEquals(2, countInstances(ConvertIntRegToFloatReg::class.java, codes))
        assertAssignmentToF(codes)
    }

    @Test
    fun shouldAssignCompareFloatFloatExpression() {
        val equalExpression = EqualExpression(0, 0, FL_3_14, FL_17_E4)
        val assignStatement = AssignStatement(0, 0, IDENT_BOOL_C, equalExpression)

        val result = assembleProgram(listOf(assignStatement))

        val codes = result.codes()
        assertEquals(1, countInstances(Je::class.java, codes))
        assertEquals(1, countInstances(CompareFloatRegWithFloatReg::class.java, codes))
        // Loading two immediate float values
        assertEquals(2, countInstances(MoveMemToFloatReg::class.java, codes))
    }

    @Test
    fun shouldAssignCompareIntFloatExpression() {
        val greaterExpression = GreaterExpression(0, 0, IL_4, FL_17_E4)
        val assignStatement = AssignStatement(0, 0, IDENT_BOOL_C, greaterExpression)

        val result = assembleProgram(listOf(assignStatement))

        val codes = result.codes()
        assertEquals(1, countInstances(Jg::class.java, codes))
        assertEquals(1, countInstances(CompareFloatRegWithFloatReg::class.java, codes))
        // Convert one int value to float
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, codes))
        // Load one float immediate
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, codes))
    }

    @Test
    fun shouldAssignCompareFloatIntExpression() {
        val lessOrEqualExpression = LessOrEqualExpression(0, 0, FL_3_14, IDE_I64_A)
        val assignStatement = AssignStatement(0, 0, IDENT_BOOL_C, lessOrEqualExpression)

        val result = assembleProgram(listOf(assignStatement))

        val codes = result.codes()
        assertEquals(1, countInstances(Jle::class.java, codes))
        assertEquals(1, countInstances(CompareFloatRegWithFloatReg::class.java, codes))
        // Convert one int variable to float
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, codes))
        // Load one float immediate
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, codes))
    }


    @Test
    fun shouldGenerateSimpleWhile() {
        val expression = EqualExpression(0, 0, IL_3, FL_3_14)
        val ps = PrintStatement(0, 0, listOf<Expression>(IL_1))
        val ws = WhileStatement(0, 0, expression, listOf<Statement>(ps))

        val result = assembleProgram(listOf<Statement>(ws))

        val codes = result.codes()
        // One for the exit code, one for the integer literal,
        // two for the boolean results, and two for the print statement
        assertEquals(6, countInstances(MoveImmToReg::class.java, codes))
        // One for the float literal
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, codes))
        // One for comparing the numbers
        assertEquals(1, countInstances(CompareFloatRegWithFloatReg::class.java, codes))
        // One for the while statement
        assertEquals(1, countInstances(Cmp::class.java, codes))
        // One for comparing the integers, and one for the while statement
        assertEquals(2, countInstances(Je::class.java, codes))
        // One for comparing the integers, and one for the while statement
        assertEquals(2, countInstances(Jmp::class.java, codes))
    }

    private fun assertAssignmentToF(codes: List<Code>) {
        assertEquals(1, codes
                .filter { it is MoveFloatRegToMem }
                .map { (it as MoveFloatRegToMem).destination }
                .filter { it == "[" + IDENT_F64_F.mappedName + "]" }
                .count())
    }
}
