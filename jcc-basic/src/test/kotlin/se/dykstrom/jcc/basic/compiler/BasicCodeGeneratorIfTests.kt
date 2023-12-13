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

import org.junit.Assert.assertEquals
import org.junit.Test
import se.dykstrom.jcc.basic.ast.PrintStatement
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_1
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_2
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_3
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_4
import se.dykstrom.jcc.common.assembly.instruction.Cmp
import se.dykstrom.jcc.common.assembly.instruction.Je
import se.dykstrom.jcc.common.assembly.instruction.Jmp
import se.dykstrom.jcc.common.assembly.instruction.MoveImmToReg
import se.dykstrom.jcc.common.ast.EqualExpression
import se.dykstrom.jcc.common.ast.Expression
import se.dykstrom.jcc.common.ast.IfStatement

class BasicCodeGeneratorIfTests : AbstractBasicCodeGeneratorTests() {

    @Test
    fun shouldGenerateIfThen() {
        val expression = EqualExpression(0, 0, IL_3, IL_4)
        val ps = PrintStatement(0, 0, listOf(IL_1))
        val ifs = IfStatement.builder(expression, ps).build()
        val result = assembleProgram(listOf(ifs))
        val lines = result.lines()

        // One for the exit code, two for the integer subexpressions, 
        // two for the boolean results, and two for the print statement
        assertEquals(7, countInstances(MoveImmToReg::class.java, lines))
        // One for comparing the integers, and one for the if statement
        assertEquals(2, countInstances(Cmp::class.java, lines))
        // One for comparing the integers, and one for the if statement
        assertEquals(2, countInstances(Je::class.java, lines))
        // One for comparing the integers
        assertEquals(1, countInstances(Jmp::class.java, lines))
    }

    @Test
    fun shouldGenerateIfThenElse() {
        val expression: Expression = EqualExpression(0, 0, IL_3, IL_4)
        val ps1 = PrintStatement(0, 0, listOf(IL_1))
        val ps2 = PrintStatement(0, 0, listOf(IL_2))
        val ifs = IfStatement.builder(expression, ps1).elseStatements(ps2).build()
        val result = assembleProgram(listOf(ifs))
        val lines = result.lines()

        // One for the exit code, two for the integer subexpressions, 
        // two for the boolean results, and four for the print statements
        assertEquals(9, countInstances(MoveImmToReg::class.java, lines))
        // One for comparing the integers, and one for the if statement
        assertEquals(2, countInstances(Cmp::class.java, lines))
        // One for comparing the integers, and one for the if statement
        assertEquals(2, countInstances(Je::class.java, lines))
        // One for comparing the integers, and one for the if statement
        assertEquals(2, countInstances(Jmp::class.java, lines))
    }

    @Test
    fun shouldGenerateIfThenElseIfElse() {
        val secondExpr = EqualExpression(0, 0, IL_3, IL_4)
        val ps1 = PrintStatement(0, 0, listOf(IL_1))
        val ps2 = PrintStatement(0, 0, listOf(IL_2))
        val secondIf = IfStatement.builder(secondExpr, ps1).elseStatements(ps2).build()
        val firstExpr = EqualExpression(0, 0, IL_1, IL_2)
        val ps3 = PrintStatement(0, 0, listOf(IL_3))
        val firstIf = IfStatement.builder(firstExpr, ps3).elseStatements(secondIf).build()
        val result = assembleProgram(listOf(firstIf))
        val lines = result.lines()

        // One for the exit code, four for the integer subexpressions, 
        // four for the boolean results, and six for the print statements
        assertEquals(15, countInstances(MoveImmToReg::class.java, lines))
        // Two for comparing the integers, and two for the if statements
        assertEquals(4, countInstances(Cmp::class.java, lines))
        // Two for comparing the integers, and two for the if statements
        assertEquals(4, countInstances(Je::class.java, lines))
        // Two for comparing the integers, and two for the if statements
        assertEquals(4, countInstances(Jmp::class.java, lines))
    }
}
