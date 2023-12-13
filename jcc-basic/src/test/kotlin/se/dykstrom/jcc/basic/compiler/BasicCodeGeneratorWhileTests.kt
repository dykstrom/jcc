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
import se.dykstrom.jcc.basic.BasicTests.Companion.IL_M1
import se.dykstrom.jcc.common.assembly.instruction.Cmp
import se.dykstrom.jcc.common.assembly.instruction.Je
import se.dykstrom.jcc.common.assembly.instruction.Jmp
import se.dykstrom.jcc.common.assembly.instruction.MoveImmToReg
import se.dykstrom.jcc.common.ast.EqualExpression
import se.dykstrom.jcc.common.ast.WhileStatement

class BasicCodeGeneratorWhileTests : AbstractBasicCodeGeneratorTests() {

    @Test
    fun shouldGenerateEmptyWhile() {
        val expression = EqualExpression(0, 0, IL_3, IL_4)
        val ws = WhileStatement(0, 0, expression, listOf())
        val result = assembleProgram(listOf(ws))
        val lines = result.lines()

        // One for the exit code, two for the integer expressions, two for the boolean results
        assertEquals(5, countInstances(MoveImmToReg::class.java, lines))
        // One for comparing the integers, and one for the while statement
        assertEquals(2, countInstances(Cmp::class.java, lines))
        // One for comparing the integers, and one for the while statement
        assertEquals(2, countInstances(Je::class.java, lines))
        // One for comparing the integers, and one for the while statement
        assertEquals(2, countInstances(Jmp::class.java, lines))
    }

    @Test
    fun shouldGenerateSimpleWhile() {
        val expression = EqualExpression(0, 0, IL_3, IL_4)
        val ps = PrintStatement(0, 0, listOf(IL_1))
        val ws = WhileStatement(0, 0, expression, listOf(ps))
        val result = assembleProgram(listOf(ws))
        val lines = result.lines()

        // One for the exit code, two for the integer expressions, 
        // two for the boolean results, and two for the print statement
        assertEquals(7, countInstances(MoveImmToReg::class.java, lines))
        // One for comparing the integers, and one for the while statement
        assertEquals(2, countInstances(Cmp::class.java, lines))
        // One for comparing the integers, and one for the while statement
        assertEquals(2, countInstances(Je::class.java, lines))
        // One for comparing the integers, and one for the while statement
        assertEquals(2, countInstances(Jmp::class.java, lines))
    }

    @Test
    fun shouldGenerateNestedWhile() {
        val ps = PrintStatement(0, 0, listOf(IL_M1))
        val innerWhile = WhileStatement(0, 0, IL_4, listOf(ps))
        val outerWhile = WhileStatement(0, 0, IL_2, listOf(innerWhile))
        val result = assembleProgram(listOf(outerWhile))
        val lines = result.lines()

        // One for the exit code, two for the integer expressions, and two for the print statement
        assertEquals(5, countInstances(MoveImmToReg::class.java, lines))
        // Two for the while statements
        assertEquals(2, countInstances(Cmp::class.java, lines))
        // Two for the while statements
        assertEquals(2, countInstances(Je::class.java, lines))
        // Two for the while statements
        assertEquals(2, countInstances(Jmp::class.java, lines))
    }
}
