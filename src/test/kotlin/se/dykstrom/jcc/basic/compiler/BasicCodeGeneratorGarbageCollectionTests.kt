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
import se.dykstrom.jcc.common.assembly.base.Code
import se.dykstrom.jcc.common.assembly.instruction.CallIndirect
import se.dykstrom.jcc.common.ast.AddExpression
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
     */
    @Test
    fun shouldAddStringLiterals() {
        val addExpression = AddExpression(0, 0, SL_ONE, SL_TWO)
        val printStatement = PrintStatement(0, 0, listOf(addExpression))

        val result = assembleProgram(listOf(printStatement))
        val codes = result.codes()

        assertEquals(0, countIndirectCalls("free", codes))
    }

    /**
     * When adding a string literal and an expression that returns a string,
     * the memory allocated when evaluating the expression should be freed.
     */
    @Test
    fun shouldAddStringLiteralAndStringExpression() {
        val addExpression1 = AddExpression(0, 0, SL_ONE, SL_TWO)
        val addExpression2 = AddExpression(0, 0, SL_ONE, addExpression1)
        val printStatement = PrintStatement(0, 0, listOf(addExpression2))

        val result = assembleProgram(listOf(printStatement))
        val codes = result.codes()

        assertEquals(1, countIndirectCalls("free", codes))
    }

    /**
     * When adding a string literal and a variable that refers to a string literal,
     * no memory should be freed after the addition.
     */
    @Test
    fun shouldAddStringLiteralAndStringLiteralVar() {
        val assignStatement = AssignStatement(0, 0, IDENT_STR_B, SL_TWO)

        val derefExpression = IdentifierDerefExpression(0, 0, IDENT_STR_B)
        val addExpression = AddExpression(0, 0, SL_ONE, derefExpression)
        val printStatement = PrintStatement(0, 0, listOf(addExpression))

        val result = assembleProgram(listOf(assignStatement, printStatement))
        val codes = result.codes()

        assertEquals(0, countIndirectCalls("free", codes))
    }

    /**
     * When adding a string literal and a variable that refers to a dynamic string,
     * no memory should be freed after the addition. But the runtime code for the GC
     * contains two calls to free (in function sweep).
     */
    @Test
    fun shouldAddStringLiteralAndStringDynamicVar() {
        val addExpression1 = AddExpression(0, 0, SL_ONE, SL_TWO)
        val assignStatement = AssignStatement(0, 0, IDENT_STR_B, addExpression1)

        val derefExpression = IdentifierDerefExpression(0, 0, IDENT_STR_B)
        val addExpression2 = AddExpression(0, 0, SL_ONE, derefExpression)
        val printStatement = PrintStatement(0, 0, listOf(addExpression2))

        val result = assembleProgram(listOf(assignStatement, printStatement))
        val codes = result.codes()

        assertEquals(2, countIndirectCalls("free", codes))
    }

    private fun countIndirectCalls(function: String, codes: List<Code>) =
            codes.filterIsInstance<CallIndirect>().filter { it.target.contains(function) }.count()
}
