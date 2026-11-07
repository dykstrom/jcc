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

package se.dykstrom.jcc.common.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.common.BaseTests.Companion.FL_0_0
import se.dykstrom.jcc.common.BaseTests.Companion.FL_1_0
import se.dykstrom.jcc.common.BaseTests.Companion.IDE_I64_A
import se.dykstrom.jcc.common.BaseTests.Companion.IL_1
import se.dykstrom.jcc.common.BaseTests.Companion.IL_7
import se.dykstrom.jcc.common.BaseTests.Companion.IL_8
import se.dykstrom.jcc.common.BaseTests.Companion.IL_M1
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.ast.IntegerLiteral.ONE
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import se.dykstrom.jcc.common.compiler.DefaultTypeManager
import se.dykstrom.jcc.common.error.InvalidValueException
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.functions.UserDefinedFunction
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.Arr
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.utils.ExpressionUtils.*

class ExpressionUtilsTests {

    private val symbolTable = SymbolTable()
    private val typeManager = DefaultTypeManager()

    @Test
    fun allShouldBeIntegerExpressions() {
        val expressions = listOf(
            IL_1,
            IDE_I64_A,
            AddExpression(IL_1, IDE_I64_A),
            NegateExpression(IL_1),
            OrExpression(IL_1, IL_7)
        )
        assertTrue(areAllIntegerExpressions(expressions, typeManager))
    }

    @Test
    fun shouldDetectDivisionByZero() {
        assertThrows<InvalidValueException> { checkDivisionByZero(IDivExpression(ONE, ZERO)) }
        assertThrows<InvalidValueException> { checkDivisionByZero(DivExpression(FL_1_0, FL_0_0)) }
    }

    @Test
    fun shouldNotDetectDivisionByZero() {
        val originalExpression = IDivExpression(ONE, IL_M1)
        val returnedExpression = checkDivisionByZero(originalExpression)
        assertEquals(originalExpression, returnedExpression)
    }

    @Test
    fun shouldNotHaveFunctionCall() {
        val negateExpression = NegateExpression(IL_1)
        val nestedNegateExpression = NegateExpression(FL_0_0)
        val mulExpression = MulExpression(FL_1_0, nestedNegateExpression)
        val addExpression = AddExpression(negateExpression, mulExpression)
        val identifier = Identifier("array", Arr.from(2, I64.INSTANCE))
        val subExpression = SubExpression(FL_1_0, FL_1_0)
        val arrayAccessExpression = ArrayAccessExpression(identifier, listOf(IL_7, subExpression))
        val lessExpression = LessExpression(IL_M1, arrayAccessExpression)
        val expression = OrExpression(addExpression, lessExpression)
        assertTrue(hasNoFunctionCall(expression))
    }

    @Test
    fun shouldHaveFunctionCall() {
        val functionIdentifier = Identifier("foo", Fun.from(listOf(I64.INSTANCE), I64.INSTANCE))
        val arrayIdentifier = Identifier("array", Arr.from(2, I64.INSTANCE))

        assertFalse(hasNoFunctionCall(FunctionCallExpression(functionIdentifier, listOf(IL_1))))
        assertFalse(hasNoFunctionCall(NegateExpression(
            0, 0, FunctionCallExpression(functionIdentifier, listOf(IL_1)))
        ))
        assertFalse(hasNoFunctionCall(SubExpression(
            0, 0, IL_7, FunctionCallExpression(functionIdentifier, listOf(IL_1)))
        ))
        assertFalse(hasNoFunctionCall(ArrayAccessExpression(
            0, 0, arrayIdentifier, listOf(IL_8, FunctionCallExpression(functionIdentifier, listOf(IL_1))))
        ))
    }

    @Test
    fun shouldNotHaveUdfFunctionCall() {
        val function = LibraryFunction("foo", listOf(I64.INSTANCE), I64.INSTANCE, ExternalFunction("foo"))
        val identifier = Identifier("foo", Fun.from(listOf(I64.INSTANCE), I64.INSTANCE))
        val expression = FunctionCallExpression(identifier, listOf(IL_1))

        symbolTable.addFunction(function)

        assertTrue(hasNoUdfFunctionCall(expression, symbolTable))
    }

    @Test
    fun shouldHaveUdfFunctionCall() {
        val functionBar = UserDefinedFunction("bar", listOf(), listOf(), I64.INSTANCE)
        val functionFoo = LibraryFunction("foo", listOf(I64.INSTANCE), I64.INSTANCE, ExternalFunction("foo"))
        val identifierBar = Identifier("bar", Fun.from(listOf(), I64.INSTANCE))
        val identifierFoo = Identifier("foo", Fun.from(listOf(I64.INSTANCE), I64.INSTANCE))
        val fceBar = FunctionCallExpression(identifierBar, listOf())
        val fceFoo = FunctionCallExpression(identifierFoo, listOf(fceBar))

        symbolTable.addFunction(functionBar)
        symbolTable.addFunction(functionFoo)

        assertFalse(hasNoUdfFunctionCall(fceFoo, symbolTable))
    }
}
