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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_17_E4
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_3_14
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_F64_F
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_1
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_2
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_3
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_4
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_F64_F
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_I64_H
import se.dykstrom.jcc.basic.ast.PrintStatement
import se.dykstrom.jcc.common.assembly.instruction.*
import se.dykstrom.jcc.common.assembly.instruction.floating.*
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.code.Line

class BasicCodeGeneratorFloatTests : AbstractBasicCodeGeneratorTests() {

    @Test
    fun shouldAssignFloatLiteral() {
        val assignStatement = AssignStatement(0, 0, INE_F64_F, FL_3_14)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        // Exit code
        assertEquals(1, countInstances(MoveImmToReg::class.java, lines))
        // Evaluating the float literal
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, lines))
        // Storing the evaluated float literal
        assertEquals(1, countInstances(MoveFloatRegToMem::class.java, lines))
    }

    @Test
    fun shouldAssignIntegerLiteralToFloatVariable() {
        val assignStatement = AssignStatement(0, 0, INE_F64_F, IL_4)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, lines))
        assertAssignmentToF(lines)
    }

    @Test
    fun shouldAssignFloatLiteralToIntegerVariable() {
        val assignStatement = AssignStatement(0, 0, INE_I64_A, FL_17_E4)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        assertEquals(1, countInstances(RoundFloatRegToIntReg::class.java, lines))
        assertAssignmentToA(lines)
    }

    @Test
    fun shouldAssignFloatExpressionToIntegerVariable() {
        val addExpression = AddExpression(0, 0, FL_3_14, FL_17_E4)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, addExpression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        assertEquals(1, countInstances(AddFloatRegToFloatReg::class.java, lines))
        assertEquals(1, countInstances(RoundFloatRegToIntReg::class.java, lines))
        assertAssignmentToA(lines)
    }

    @Test
    fun shouldAssignFloatVariableToIntegerVariable() {
        val derefExpression = IdentifierDerefExpression(0, 0, IDENT_F64_F)
        val assignStatement = AssignStatement(0, 0, INE_I64_A, derefExpression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        assertEquals(1, countInstances(RoundFloatRegToIntReg::class.java, lines))
        assertAssignmentToA(lines)
    }

    @Test
    fun shouldAssignNegatedFloatExpression() {
        val expression = NegateExpression(0, 0, FL_17_E4)
        val statement = AssignStatement(0, 0, INE_F64_F, expression)

        val result = assembleProgram(listOf(statement))
        val lines = result.lines()

        // Move float result to current register
        assertEquals(1, countInstances(XorFloatRegWithFloatReg::class.java, lines))
        assertEquals(1, lines
            .filterIsInstance<MoveFloatRegToMem>()
            .map { code -> code.destination }
            .count { name -> name == "[" + IDENT_F64_F.mappedName + "]" })
    }

    @Test
    fun shouldAssignAddFloatFloatExpression() {
        val addExpression = AddExpression(0, 0, FL_3_14, FL_17_E4)
        val assignStatement = AssignStatement(0, 0, INE_F64_F, addExpression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        assertEquals(1, countInstances(AddFloatRegToFloatReg::class.java, lines))
        assertAssignmentToF(lines)
    }

    @Test
    fun shouldAssignAddFloatIntegerExpression() {
        val addExpression = AddExpression(0, 0, FL_3_14, IL_3)
        val assignStatement = AssignStatement(0, 0, INE_F64_F, addExpression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        assertEquals(1, countInstances(AddFloatRegToFloatReg::class.java, lines))
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, lines))
        assertAssignmentToF(lines)
    }

    @Test
    fun shouldAssignAddIntegerFloatExpression() {
        val addExpression = AddExpression(0, 0, IL_3, FL_3_14)
        val assignStatement = AssignStatement(0, 0, INE_F64_F, addExpression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        assertEquals(1, countInstances(AddFloatRegToFloatReg::class.java, lines))
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, lines))
        assertAssignmentToF(lines)
    }

    @Test
    fun shouldAssignComplexAddExpression() {
        val addExpression1 = AddExpression(0, 0, IL_3, FL_3_14)
        val addExpression2 = AddExpression(0, 0, IL_2, FL_17_E4)
        val addExpression3 = AddExpression(0, 0, addExpression1, addExpression2)
        val assignStatement = AssignStatement(0, 0, INE_F64_F, addExpression3)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        assertEquals(3, countInstances(AddFloatRegToFloatReg::class.java, lines))
        assertEquals(2, countInstances(ConvertIntRegToFloatReg::class.java, lines))
        assertAssignmentToF(lines)
    }

    @Test
    fun shouldAssignSubFloatFloatExpression() {
        val subExpression = SubExpression(0, 0, FL_3_14, FL_17_E4)
        val assignStatement = AssignStatement(0, 0, INE_F64_F, subExpression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        assertEquals(1, countInstances(SubFloatRegFromFloatReg::class.java, lines))
        assertAssignmentToF(lines)
    }

    @Test
    fun shouldAssignSubFloatIntegerExpression() {
        val subExpression = SubExpression(0, 0, FL_3_14, IL_1)
        val assignStatement = AssignStatement(0, 0, INE_F64_F, subExpression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        assertEquals(1, countInstances(SubFloatRegFromFloatReg::class.java, lines))
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, lines))
        assertAssignmentToF(lines)
    }

    @Test
    fun shouldAssignSubIntegerFloatExpression() {
        val subExpression = SubExpression(0, 0, IL_4, FL_17_E4)
        val assignStatement = AssignStatement(0, 0, INE_F64_F, subExpression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        assertEquals(1, countInstances(SubFloatRegFromFloatReg::class.java, lines))
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, lines))
        assertAssignmentToF(lines)
    }

    @Test
    fun shouldAssignMulFloatFloatExpression() {
        val mulExpression = MulExpression(0, 0, FL_3_14, FL_17_E4)
        val assignStatement = AssignStatement(0, 0, INE_F64_F, mulExpression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        assertEquals(1, countInstances(MulFloatRegWithFloatReg::class.java, lines))
        assertAssignmentToF(lines)
    }

    @Test
    fun shouldAssignDivFloatFloatExpression() {
        val divExpression = DivExpression(0, 0, FL_3_14, FL_17_E4)
        val assignStatement = AssignStatement(0, 0, INE_F64_F, divExpression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        assertEquals(1, countInstances(DivFloatRegWithFloatReg::class.java, lines))
        assertAssignmentToF(lines)
    }

    @Test
    fun shouldAssignDivFloatIntegerExpression() {
        val divExpression = DivExpression(0, 0, FL_3_14, IL_3)
        val assignStatement = AssignStatement(0, 0, INE_F64_F, divExpression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        assertEquals(1, countInstances(DivFloatRegWithFloatReg::class.java, lines))
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, lines))
        assertAssignmentToF(lines)
    }

    @Test
    fun shouldAssignDivIntegerFloatExpression() {
        val divExpression = DivExpression(0, 0, IL_2, FL_3_14)
        val assignStatement = AssignStatement(0, 0, INE_F64_F, divExpression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        assertEquals(1, countInstances(DivFloatRegWithFloatReg::class.java, lines))
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, lines))
        assertAssignmentToF(lines)
    }

    @Test
    fun shouldAssignDivIntegerIntegerExpression() {
        val divExpression = DivExpression(0, 0, IL_2, IL_4)
        val assignStatement = AssignStatement(0, 0, INE_F64_F, divExpression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        assertEquals(1, countInstances(DivFloatRegWithFloatReg::class.java, lines))
        assertEquals(2, countInstances(ConvertIntRegToFloatReg::class.java, lines))
        assertAssignmentToF(lines)
    }

    @Test
    fun shouldAssignCompareFloatFloatExpression() {
        val equalExpression = EqualExpression(0, 0, FL_3_14, FL_17_E4)
        val assignStatement = AssignStatement(0, 0, INE_I64_H, equalExpression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        assertEquals(1, countInstances(Je::class.java, lines))
        assertEquals(1, countInstances(CompareFloatRegWithFloatReg::class.java, lines))
        // Loading two immediate float values
        assertEquals(2, countInstances(MoveMemToFloatReg::class.java, lines))
    }

    @Test
    fun shouldAssignCompareIntFloatExpression() {
        val greaterExpression = GreaterExpression(0, 0, IL_4, FL_17_E4)
        val assignStatement = AssignStatement(0, 0, INE_I64_H, greaterExpression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        assertEquals(1, countInstances(Ja::class.java, lines))
        assertEquals(1, countInstances(CompareFloatRegWithFloatReg::class.java, lines))
        // Convert one int value to float
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, lines))
        // Load one float immediate
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, lines))
    }

    @Test
    fun shouldAssignCompareFloatIntExpression() {
        val lessOrEqualExpression = LessOrEqualExpression(0, 0, FL_3_14, IDE_I64_A)
        val assignStatement = AssignStatement(0, 0, INE_I64_H, lessOrEqualExpression)

        val result = assembleProgram(listOf(assignStatement))
        val lines = result.lines()

        assertEquals(1, countInstances(Jbe::class.java, lines))
        assertEquals(1, countInstances(CompareFloatRegWithFloatReg::class.java, lines))
        // Convert one int variable to float
        assertEquals(1, countInstances(ConvertIntRegToFloatReg::class.java, lines))
        // Load one float immediate
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, lines))
    }

    @Test
    fun shouldGenerateSimpleWhile() {
        val expression = EqualExpression(0, 0, IL_3, FL_3_14)
        val ps = PrintStatement(0, 0, listOf(IL_1))
        val ws = WhileStatement(0, 0, expression, listOf(ps))

        val result = assembleProgram(listOf(ws))
        val lines = result.lines()

        // One for the exit code, one for the integer literal,
        // two for the boolean results, and two for the print statement
        assertEquals(6, countInstances(MoveImmToReg::class.java, lines))
        // One for the float literal
        assertEquals(1, countInstances(MoveMemToFloatReg::class.java, lines))
        // One for comparing the numbers
        assertEquals(1, countInstances(CompareFloatRegWithFloatReg::class.java, lines))
        // One for the while statement
        assertEquals(1, countInstances(Cmp::class.java, lines))
        // One for comparing the integers, and one for the while statement
        assertEquals(2, countInstances(Je::class.java, lines))
        // One for comparing the integers, and one for the while statement
        assertEquals(2, countInstances(Jmp::class.java, lines))
    }

    private fun assertAssignmentToF(lines: List<Line>) {
        assertEquals(1, lines
                .filterIsInstance<MoveFloatRegToMem>()
                .map { it.destination }
                .count { it == "[" + IDENT_F64_F.mappedName + "]" })
    }

    private fun assertAssignmentToA(lines: List<Line>) {
        assertEquals(1, lines
                .filterIsInstance<MoveRegToMem>()
                .map { it.destination }
                .count { it == "[" + IDENT_I64_A.mappedName + "]" })
    }
}
