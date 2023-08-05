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

import org.junit.Test
import se.dykstrom.jcc.basic.ast.PrintStatement
import se.dykstrom.jcc.common.intermediate.Line
import se.dykstrom.jcc.common.assembly.instruction.CallIndirect
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.ArrayAccessExpression
import se.dykstrom.jcc.common.ast.AssignStatement
import se.dykstrom.jcc.common.ast.IdentifierDerefExpression
import kotlin.test.assertEquals

/**
 * Tests features related to garbage collection and memory management in code generation.
 *
 * @author Johan Dykstrom
 */
class BasicCodeGeneratorGarbageCollectionTests : AbstractBasicCodeGeneratorTest() {

    /**
     * When adding two string literals, no memory should be freed after the addition.
     * But after calling the print function, the memory allocated by the addition itself
     * should be freed.
     */
    @Test
    fun shouldAddStringLiterals() {
        val addExpression = AddExpression(0, 0, SL_ONE, SL_TWO)
        val printStatement = PrintStatement(0, 0, listOf(addExpression))

        val result = assembleProgram(listOf(printStatement))
        val lines = result.lines()

        assertEquals(1, countIndirectCalls("free", lines))
    }

    /**
     * When adding a string literal and an expression that returns a string,
     * the memory allocated when evaluating the expression should be freed
     * after the addition. After calling the print function, the memory
     * allocated by the addition itself should be freed.
     */
    @Test
    fun shouldAddStringLiteralAndStringExpression() {
        val addExpression1 = AddExpression(0, 0, SL_ONE, SL_TWO)
        val addExpression2 = AddExpression(0, 0, SL_ONE, addExpression1)
        val printStatement = PrintStatement(0, 0, listOf(addExpression2))

        val result = assembleProgram(listOf(printStatement))
        val lines = result.lines()

        assertEquals(2, countIndirectCalls("free", lines))
    }

    /**
     * When adding a string literal and a variable that refers to a string literal,
     * no memory should be freed after the addition. But after calling the print
     * function, the memory allocated by the addition itself should be freed.
     */
    @Test
    fun shouldAddStringLiteralAndStringLiteralVar() {
        val assignStatement = AssignStatement(0, 0, NAME_B, SL_TWO)

        val derefExpression = IdentifierDerefExpression(0, 0, IDENT_STR_B)
        val addExpression = AddExpression(0, 0, SL_ONE, derefExpression)
        val printStatement = PrintStatement(0, 0, listOf(addExpression))

        val result = assembleProgram(listOf(assignStatement, printStatement))
        val lines = result.lines()

        assertEquals(1, countIndirectCalls("free", lines))
    }

    /**
     * When adding a string literal and an array element that refers to a string literal,
     * no memory should be freed after the addition. But after calling the print
     * function, the memory allocated by the addition itself should be freed.
     */
    @Test
    fun shouldAddStringLiteralAndStringLiteralArrayElement() {
        val arrayExpression = ArrayAccessExpression(0, 0, IDENT_ARR_STR_S, listOf(IL_0))
        val assignStatement = AssignStatement(0, 0, arrayExpression, SL_TWO)

        val addExpression = AddExpression(0, 0, SL_ONE, arrayExpression)
        val printStatement = PrintStatement(0, 0, listOf(addExpression))

        val result = assembleProgram(listOf(assignStatement, printStatement))
        val lines = result.lines()

        assertEquals(1, countIndirectCalls("free", lines))
    }

    /**
     * When adding a string literal and a variable that refers to a dynamic string,
     * no memory should be freed after the addition. But after calling the print
     * function, the memory allocated by the addition itself should be freed. And
     * the runtime code for the GC contains two calls to free (in function sweep).
     */
    @Test
    fun shouldAddStringLiteralAndStringDynamicVar() {
        val addExpression1 = AddExpression(0, 0, SL_ONE, SL_TWO)
        val assignStatement = AssignStatement(0, 0, NAME_B, addExpression1)

        val derefExpression = IdentifierDerefExpression(0, 0, IDENT_STR_B)
        val addExpression2 = AddExpression(0, 0, SL_ONE, derefExpression)
        val printStatement = PrintStatement(0, 0, listOf(addExpression2))

        val result = assembleProgram(listOf(assignStatement, printStatement))
        val lines = result.lines()

        assertEquals(3, countIndirectCalls("free", lines))
    }

    /**
     * When printing many string literals, no memory should be freed after calling the
     * print function.
     */
    @Test
    fun shouldPrintManyStringLiterals() {
        val printStatement = PrintStatement(0, 0, listOf(SL_ONE, SL_TWO, SL_FOO, SL_BAR))

        val result = assembleProgram(listOf(printStatement))
        val lines = result.lines()

        assertEquals(0, countIndirectCalls("free", lines))
    }

    /**
     * When printing one string addition (transferred in a register) and many string literals,
     * the memory allocated in the addition should be freed after calling the print function.
     */
    @Test
    fun shouldPrintAdditionAndManyStringLiterals() {
        val addExpression = AddExpression(0, 0, SL_ONE, SL_TWO)
        val printStatement = PrintStatement(0, 0, listOf(addExpression, SL_ONE, SL_TWO, SL_FOO, SL_BAR))

        val result = assembleProgram(listOf(printStatement))
        val lines = result.lines()

        assertEquals(1, countIndirectCalls("free", lines))
    }

    /**
     * When printing many string literals and one string addition (transferred on the stack),
     * the memory allocated in the addition should be freed after calling the print function.
     */
    @Test
    fun shouldPrintManyStringLiteralsAndAddition() {
        val addExpression = AddExpression(0, 0, SL_ONE, SL_TWO)
        val printStatement = PrintStatement(0, 0, listOf(SL_ONE, SL_TWO, SL_FOO, SL_BAR, addExpression))

        val result = assembleProgram(listOf(printStatement))
        val lines = result.lines()

        assertEquals(1, countIndirectCalls("free", lines))
    }

    /**
     * When printing one string addition (transferred in a register), many string literals,
     * and one more string addition (transferred on the stack), the memory allocated in the
     * two additions should be freed after calling the print function.
     */
    @Test
    fun shouldPrintAdditionAndManyStringLiteralsAndAddition() {
        val addExpression1 = AddExpression(0, 0, SL_ONE, SL_TWO)
        val addExpression2 = AddExpression(0, 0, SL_FOO, SL_BAR)
        val printStatement = PrintStatement(0, 0, listOf(addExpression1, SL_ONE, SL_TWO, SL_FOO, SL_BAR, addExpression2))

        val result = assembleProgram(listOf(printStatement))
        val lines = result.lines()

        assertEquals(2, countIndirectCalls("free", lines))
    }

    /**
     * When printing four string additions (transferred both in registers and on the stack), the memory
     * allocated in all the additions should be freed after calling the print function.
     */
    @Test
    fun shouldPrintManyAdditions() {
        val addExpression1 = AddExpression(0, 0, SL_ONE, SL_TWO)
        val addExpression2 = AddExpression(0, 0, SL_FOO, SL_BAR)
        val addExpression3 = AddExpression(0, 0, SL_ONE, SL_TWO)
        val addExpression4 = AddExpression(0, 0, SL_FOO, SL_BAR)
        val printStatement = PrintStatement(0, 0, listOf(addExpression1, addExpression2, addExpression3, addExpression4))

        val result = assembleProgram(listOf(printStatement))
        val lines = result.lines()

        assertEquals(4, countIndirectCalls("free", lines))
    }

    private fun countIndirectCalls(function: String, lines: List<Line>) =
            lines.filterIsInstance<CallIndirect>().count { it.target.contains(function) }
}
