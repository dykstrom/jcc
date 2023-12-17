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
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.ast.IntegerLiteral.ONE
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import se.dykstrom.jcc.common.compiler.DefaultTypeManager
import se.dykstrom.jcc.common.error.InvalidValueException
import se.dykstrom.jcc.common.optimization.DefaultAstExpressionOptimizer
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.utils.ExpressionUtils.*

class ExpressionUtilsTests {

    @Test
    fun allShouldBeIntegerExpressions() {
        val expressions = listOf(
            IL_1,
            IDE_I64_A,
            AddExpression(0, 0, IL_1, IDE_I64_A),
            NegateExpression(0, 0, IL_1),
            OrExpression(0, 0, IL_1, IL_7)
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
            AddExpression(0, 0, IL_1, IL_1),
            NotExpression(0, 0, IL_1),
            NegateExpression(0, 0, IdentifierDerefExpression(0, 0, IDENT_I64_A))
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
        assertFalse(areAllConstantExpressions(listOf(AddExpression(0, 0, IL_1, IDE_I64_A)), symbolTable))
        assertFalse(areAllConstantExpressions(
            listOf(FunctionCallExpression(0, 0, Identifier("", I64.INSTANCE), listOf())),
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
        assertEquals("8", evaluateExpression(AddExpression(0, 0, IL_7, IL_1), symbolTable, OPTIMIZER, EXTRACT_VALUE))
        assertEquals("15.0", evaluateExpression(AddExpression(0, 0, FL_1_0, IL_14), symbolTable, OPTIMIZER, EXTRACT_VALUE))
        assertEquals("OneOne", evaluateExpression(AddExpression(0, 0, SL_ONE, SL_ONE), symbolTable, OPTIMIZER, EXTRACT_VALUE))
    }

    @Test
    fun shouldEvaluateGenericBitwiseExpressions() {
        // Given
        val symbolTable = SymbolTable()

        // When & Then
        assertEquals("1", evaluateExpression(AndExpression(0, 0, IL_7, IL_1), symbolTable, OPTIMIZER, EXTRACT_VALUE))
        assertEquals("15", evaluateExpression(OrExpression(0, 0, IL_7, IL_8), symbolTable, OPTIMIZER, EXTRACT_VALUE))
        assertEquals("6", evaluateExpression(XorExpression(0, 0, IL_7, IL_1), symbolTable, OPTIMIZER, EXTRACT_VALUE))
        assertEquals("0", evaluateExpression(NotExpression(0, 0, IL_M1), symbolTable, OPTIMIZER, EXTRACT_VALUE))
    }

    @Test
    fun shouldEvaluateGenericExpressionIncludingConstant() {
        // Given
        val symbolTable = SymbolTable()
        symbolTable.addConstant(IDENT_I64_A, "6")

        val mulExpression = MulExpression(0, 0, NEG_1, IL_8) // -1 * 8 = -8
        val addExpression = AddExpression(0, 0, mulExpression, IDE_I64_A) // -8 + 6 = -2

        // When & Then
        assertEquals("-2", evaluateExpression(addExpression, symbolTable, OPTIMIZER, EXTRACT_VALUE))
    }

    @Test
    fun shouldDetectDivisionByZero() {
        assertThrows<InvalidValueException> { checkDivisionByZero(IDivExpression(0, 0, ONE, ZERO)) }
        assertThrows<InvalidValueException> { checkDivisionByZero(DivExpression(0, 0, FL_1_0, FL_0_0)) }
    }

    @Test
    fun shouldNotDetectDivisionByZero() {
        val originalExpression = IDivExpression(0, 0, ONE, IL_M1)
        val returnedExpression = checkDivisionByZero(originalExpression)
        assertEquals(originalExpression, returnedExpression)
    }

    companion object {
        private val FL_0_0 = FloatLiteral(0, 0, "0.0")
        private val FL_1_0 = FloatLiteral(0, 0, "1.0")
        private val IL_1 = IntegerLiteral(0, 0, "1")
        private val IL_M1 = IntegerLiteral(0, 0, "-1")
        private val IL_7 = IntegerLiteral(0, 0, "7")
        private val IL_8 = IntegerLiteral(0, 0, "8")
        private val IL_14 = IntegerLiteral(0, 0, "14")
        private val SL_ONE = StringLiteral(0, 0, "One")

        private val IDENT_F64_F = Identifier("f", F64.INSTANCE)
        private val IDENT_I64_A = Identifier("a%", I64.INSTANCE)

        private val IDE_F64_F: Expression = IdentifierDerefExpression(0, 0, IDENT_F64_F)
        private val IDE_I64_A: Expression = IdentifierDerefExpression(0, 0, IDENT_I64_A)

        private val ADD_1_7: Expression = AddExpression(0, 0, IL_1, IL_7)
        private val ADD_7_7: Expression = AddExpression(0, 0, IL_7, IL_7)
        private val ADD_1_7_7: Expression = AddExpression(0, 0, IL_1, AddExpression(0, 0, IL_7, IL_7))
        private val SUB_7_7: Expression = SubExpression(0, 0, IL_7, IL_7)
        private val MUL_7_7: Expression = MulExpression(0, 0, IL_7, IL_7)
        private val IDIV_14_7: Expression = IDivExpression(0, 0, IL_14, IL_7)
        private val NEG_1: Expression = NegateExpression(0, 0, IL_1)

        private val NOT_M1: Expression = NotExpression(0, 0, IL_M1)

        private val TYPE_MANAGER = DefaultTypeManager()
        private val OPTIMIZER = DefaultAstExpressionOptimizer(TYPE_MANAGER)
        private val EXTRACT_VALUE: (Expression) -> String = { (it as LiteralExpression).value }
    }
}
