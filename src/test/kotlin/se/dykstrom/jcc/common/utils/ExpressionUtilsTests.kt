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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.compiler.DefaultTypeManager
import se.dykstrom.jcc.common.optimization.DefaultAstExpressionOptimizer
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.utils.ExpressionUtils.*
import kotlin.test.assertEquals

class ExpressionUtilsTests {

    @Test
    fun allShouldBeIntegerExpressions() {
        val expressions = listOf(IL_1, IDE_I64_A, AddExpression(0, 0, IL_1, IDE_I64_A))
        assertTrue(areAllIntegerExpressions(expressions, TYPE_MANAGER))
    }

    @Ignore("needs a more advanced default type manager")
    @Test
    fun allShouldNotBeIntegerExpressions() {
        assertFalse(areAllIntegerExpressions(listOf(SL_ONE), TYPE_MANAGER))
        assertFalse(areAllIntegerExpressions(listOf(IDE_F64_F), TYPE_MANAGER))
        assertFalse(areAllIntegerExpressions(listOf(AddExpression(0, 0, IL_1, FL_1_00)), TYPE_MANAGER))
    }

    @Test
    fun allShouldBeConstantExpressions() {
        val expressions = listOf(IL_1, AddExpression(0, 0, IL_1, IL_1), NotExpression(0, 0, BL_TRUE))
        assertTrue(areAllConstantExpressions(expressions))
    }

    @Test
    fun allShouldNotBeConstantExpressions() {
        assertFalse(areAllConstantExpressions(listOf(IDE_F64_F)))
        assertFalse(areAllConstantExpressions(listOf(AddExpression(0, 0, IL_1, IDE_I64_A))))
        assertFalse(areAllConstantExpressions(listOf(FunctionCallExpression(0, 0, Identifier("", I64.INSTANCE), listOf()))))
    }

    @Test
    fun shouldEvaluateIntegerLiterals() {
        assertEquals(listOf(), evaluateConstantIntegerExpressions(listOf(), OPTIMIZER))
        assertEquals(listOf(1L), evaluateConstantIntegerExpressions(listOf(IL_1), OPTIMIZER))
        assertEquals(listOf(1L, 7L, 1L), evaluateConstantIntegerExpressions(listOf(IL_1, IL_7, IL_1), OPTIMIZER))
    }

    @Test
    fun shouldEvaluateAddExpressions() {
        assertEquals(listOf(8L), evaluateConstantIntegerExpressions(listOf(ADD_1_7), OPTIMIZER))
        assertEquals(listOf(8L, 14L, 14L), evaluateConstantIntegerExpressions(listOf(ADD_1_7, ADD_7_7, ADD_7_7), OPTIMIZER))
        assertEquals(listOf(14L, 15L), evaluateConstantIntegerExpressions(listOf(ADD_7_7, ADD_1_7_7), OPTIMIZER))
    }

    @Test
    fun shouldEvaluateSubExpressions() {
        assertEquals(listOf(0L), evaluateConstantIntegerExpressions(listOf(SUB_7_7), OPTIMIZER))
    }

    @Test
    fun shouldEvaluateMulExpressions() {
        assertEquals(listOf(49L), evaluateConstantIntegerExpressions(listOf(MUL_7_7), OPTIMIZER))
    }

    @Test
    fun shouldEvaluateIDivExpressions() {
        assertEquals(listOf(2L), evaluateConstantIntegerExpressions(listOf(IDIV_14_7), OPTIMIZER))
    }

    @Test
    fun shouldEvaluateMixedExpressions() {
        assertEquals(listOf(2L, 49L, 0L, 15L), evaluateConstantIntegerExpressions(listOf(IDIV_14_7, MUL_7_7, SUB_7_7, ADD_1_7_7), OPTIMIZER))
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldNotEvaluateFloatExpressions() {
        evaluateConstantIntegerExpressions(listOf(FL_1_00), OPTIMIZER)
    }

    @Test(expected = IllegalArgumentException::class)
    fun shouldNotEvaluateDerefExpressions() {
        evaluateConstantIntegerExpressions(listOf(IDE_I64_A), OPTIMIZER)
    }

    companion object {
        private val FL_1_00 = FloatLiteral(0, 0, "1.00")
        private val IL_1 = IntegerLiteral(0, 0, "1")
        private val IL_7 = IntegerLiteral(0, 0, "7")
        private val IL_14 = IntegerLiteral(0, 0, "14")
        private val SL_ONE = StringLiteral(0, 0, "One")
        private val BL_TRUE = BooleanLiteral(0, 0, "true")

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

        private val TYPE_MANAGER = DefaultTypeManager()
        private val OPTIMIZER = DefaultAstExpressionOptimizer(TYPE_MANAGER)
    }
}
