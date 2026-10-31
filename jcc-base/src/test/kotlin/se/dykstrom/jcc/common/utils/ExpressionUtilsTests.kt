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
import se.dykstrom.jcc.common.BaseTests.Companion.IDENT_F64_F
import se.dykstrom.jcc.common.BaseTests.Companion.IDENT_I64_A
import se.dykstrom.jcc.common.BaseTests.Companion.IDE_F64_F
import se.dykstrom.jcc.common.BaseTests.Companion.IDE_I64_A
import se.dykstrom.jcc.common.BaseTests.Companion.IL_1
import se.dykstrom.jcc.common.BaseTests.Companion.IL_14
import se.dykstrom.jcc.common.BaseTests.Companion.IL_7
import se.dykstrom.jcc.common.BaseTests.Companion.IL_8
import se.dykstrom.jcc.common.BaseTests.Companion.IL_M1
import se.dykstrom.jcc.common.BaseTests.Companion.SL_ONE
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.ast.IntegerLiteral.ONE
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import se.dykstrom.jcc.common.compiler.DefaultTypeManager
import se.dykstrom.jcc.common.error.InvalidValueException
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.functions.UserDefinedFunction
import se.dykstrom.jcc.common.optimization.DefaultAstExpressionOptimizer
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.*
import se.dykstrom.jcc.common.utils.ExpressionUtils.*

class ExpressionUtilsTests {

    private val symbolTable: SymbolTable = SymbolTable()

    @Test
    fun allShouldBeIntegerExpressions() {
        val expressions = listOf(
            IL_1,
            IDE_I64_A,
            AddExpression(IL_1, IDE_I64_A),
            NegateExpression(IL_1),
            OrExpression(IL_1, IL_7)
        )
        assertTrue(areAllIntegerExpressions(expressions, TYPE_MANAGER))
    }

    @Test
    fun allShouldBeConstantExpressions() {
        // Given
        val symbolTable = SymbolTable()
        symbolTable.addConstant(IDENT_I64_A, "7")
        val expressions = listOf(
            IL_1,
            AddExpression(IL_1, IL_1),
            NotExpression(IL_1),
            NegateExpression(IdentifierDerefExpression(IDENT_I64_A))
        )

        // When & Then
        assertTrue(areAllConstantExpressions(expressions, symbolTable))
    }

    @Test
    fun allShouldNotBeConstantExpressions() {
        // Given
        val symbolTable = SymbolTable()
        symbolTable.addVariable(IDENT_I64_A)
        symbolTable.addVariable(IDENT_F64_F)

        // When & Then
        assertFalse(areAllConstantExpressions(listOf(IDE_F64_F), symbolTable))
        assertFalse(areAllConstantExpressions(listOf(AddExpression(IL_1, IDE_I64_A)), symbolTable))
        assertFalse(areAllConstantExpressions(
            listOf(FunctionCallExpression(Identifier("", I64.INSTANCE), listOf())),
            symbolTable
        ))
    }

    @Test
    fun shouldEvaluateIntegerLiterals() {
        val symbolTable = SymbolTable()
        assertEquals(listOf<Long>(), evaluateIntegerExpressions(listOf(), symbolTable, OPTIMIZER))
        assertEquals(listOf(1L), evaluateIntegerExpressions(listOf(IL_1), symbolTable, OPTIMIZER))
        assertEquals(listOf(1L, 7L, 1L), evaluateIntegerExpressions(listOf(IL_1, IL_7, IL_1), symbolTable, OPTIMIZER))
    }

    @Test
    fun shouldEvaluateAddExpressions() {
        val symbolTable = SymbolTable()
        assertEquals(listOf(8L), evaluateIntegerExpressions(listOf(ADD_1_7), symbolTable, OPTIMIZER))
        assertEquals(listOf(8L, 14L, 14L), evaluateIntegerExpressions(listOf(ADD_1_7, ADD_7_7, ADD_7_7), symbolTable, OPTIMIZER))
        assertEquals(listOf(14L, 15L), evaluateIntegerExpressions(listOf(ADD_7_7, ADD_1_7_7), symbolTable, OPTIMIZER))
    }

    @Test
    fun shouldEvaluateSubExpressions() {
        val symbolTable = SymbolTable()
        assertEquals(listOf(0L), evaluateIntegerExpressions(listOf(SUB_7_7), symbolTable, OPTIMIZER))
    }

    @Test
    fun shouldEvaluateMulExpressions() {
        val symbolTable = SymbolTable()
        assertEquals(listOf(49L), evaluateIntegerExpressions(listOf(MUL_7_7), symbolTable, OPTIMIZER))
    }

    @Test
    fun shouldEvaluateIDivExpressions() {
        val symbolTable = SymbolTable()
        assertEquals(listOf(2L), evaluateIntegerExpressions(listOf(IDIV_14_7), symbolTable, OPTIMIZER))
    }

    @Test
    fun shouldEvaluateNegateExpressions() {
        val symbolTable = SymbolTable()
        assertEquals(listOf(-1L), evaluateIntegerExpressions(listOf(NEG_1), symbolTable, OPTIMIZER))
    }

    @Test
    fun shouldEvaluateNotExpressions() {
        val symbolTable = SymbolTable()
        assertEquals(listOf(0L), evaluateIntegerExpressions(listOf(NOT_M1), symbolTable, OPTIMIZER))
    }

    @Test
    fun shouldEvaluateDerefConstantExpressions() {
        // Given
        val symbolTable = SymbolTable()
        symbolTable.addConstant(IDENT_I64_A, "-1")

        // When & Then
        assertEquals(listOf(-1L), evaluateIntegerExpressions(listOf(IDE_I64_A), symbolTable, OPTIMIZER))
    }

    @Test
    fun shouldEvaluateMixedExpressions() {
        val symbolTable = SymbolTable()
        assertEquals(listOf(2L, 49L, 0L, 15L), evaluateIntegerExpressions(
            listOf(IDIV_14_7, MUL_7_7, SUB_7_7, ADD_1_7_7),
            symbolTable,
            OPTIMIZER
        ))
    }

    @Test
    fun shouldNotEvaluateFloatExpressions() {
        val symbolTable = SymbolTable()
        assertThrows<IllegalArgumentException> {
            evaluateIntegerExpressions(listOf(FL_1_0), symbolTable, OPTIMIZER)
        }
    }

    @Test
    fun shouldNotEvaluateDerefVariableExpressions() {
        // Given
        val symbolTable = SymbolTable()
        symbolTable.addVariable(IDENT_I64_A)

        // When & Then
        assertThrows<IllegalArgumentException> {
            evaluateIntegerExpressions(listOf(IDE_I64_A), symbolTable, OPTIMIZER)
        }
    }

    @Test
    fun shouldEvaluateGenericLiteral() {
        // Given
        val symbolTable = SymbolTable()

        // When & Then
        assertEquals("1", evaluateExpression(IL_1, symbolTable, OPTIMIZER, EXTRACT_VALUE))
        assertEquals("1.0", evaluateExpression(FL_1_0, symbolTable, OPTIMIZER, EXTRACT_VALUE))
        assertEquals("One", evaluateExpression(SL_ONE, symbolTable, OPTIMIZER, EXTRACT_VALUE))
    }

    @Test
    fun shouldEvaluateGenericAddExpression() {
        // Given
        val symbolTable = SymbolTable()

        // When & Then
        assertEquals("8", evaluateExpression(AddExpression(IL_7, IL_1), symbolTable, OPTIMIZER, EXTRACT_VALUE))
        assertEquals("15.0", evaluateExpression(AddExpression(FL_1_0, IL_14), symbolTable, OPTIMIZER, EXTRACT_VALUE))
        assertEquals("OneOne", evaluateExpression(AddExpression(SL_ONE, SL_ONE), symbolTable, OPTIMIZER, EXTRACT_VALUE))
    }

    @Test
    fun shouldEvaluateGenericBitwiseExpressions() {
        // Given
        val symbolTable = SymbolTable()

        // When & Then
        assertEquals("1", evaluateExpression(AndExpression(IL_7, IL_1), symbolTable, OPTIMIZER, EXTRACT_VALUE))
        assertEquals("15", evaluateExpression(OrExpression(IL_7, IL_8), symbolTable, OPTIMIZER, EXTRACT_VALUE))
        assertEquals("6", evaluateExpression(XorExpression(IL_7, IL_1), symbolTable, OPTIMIZER, EXTRACT_VALUE))
        assertEquals("0", evaluateExpression(NotExpression(IL_M1), symbolTable, OPTIMIZER, EXTRACT_VALUE))
    }

    @Test
    fun shouldEvaluateGenericExpressionIncludingConstant() {
        // Given
        val symbolTable = SymbolTable()
        symbolTable.addConstant(IDENT_I64_A, "6")

        val mulExpression = MulExpression(NEG_1, IL_8) // -1 * 8 = -8
        val addExpression = AddExpression(mulExpression, IDE_I64_A) // -8 + 6 = -2

        // When & Then
        assertEquals("-2", evaluateExpression(addExpression, symbolTable, OPTIMIZER, EXTRACT_VALUE))
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

    companion object {
        private val ADD_1_7: Expression = AddExpression(IL_1, IL_7)
        private val ADD_7_7: Expression = AddExpression(IL_7, IL_7)
        private val ADD_1_7_7: Expression = AddExpression(IL_1, AddExpression(IL_7, IL_7))
        private val SUB_7_7: Expression = SubExpression(IL_7, IL_7)
        private val MUL_7_7: Expression = MulExpression(IL_7, IL_7)
        private val IDIV_14_7: Expression = IDivExpression(IL_14, IL_7)
        private val NEG_1: Expression = NegateExpression(IL_1)

        private val NOT_M1: Expression = NotExpression(IL_M1)

        private val TYPE_MANAGER = DefaultTypeManager()
        private val OPTIMIZER = DefaultAstExpressionOptimizer(TYPE_MANAGER)
        private val EXTRACT_VALUE: (Expression) -> String = { (it as LiteralExpression).value }
    }
}
