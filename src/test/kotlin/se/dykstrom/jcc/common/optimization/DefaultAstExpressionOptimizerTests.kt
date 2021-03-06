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

package se.dykstrom.jcc.common.optimization

import org.junit.Assert.assertEquals
import org.junit.Test
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.compiler.DefaultTypeManager
import se.dykstrom.jcc.common.error.InvalidException
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier

/**
 * Tests class `DefaultAstExpressionOptimizer`.
 *
 * @author Johan Dykstrom
 * @see DefaultAstExpressionOptimizer
 */
class DefaultAstExpressionOptimizerTests {

    @Test
    fun shouldReplaceAddIntegerLiteralsWithOneLiteral() {
        // Given
        val addExpression = AddExpression(0, 0, IL_2, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression)

        // Then
        assertEquals(IL_3, optimizedExpression)
    }

    @Test
    fun shouldReplaceAddZeroWithOneLiteral() {
        // Given
        val addExpression = AddExpression(0, 0, IL_0, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression)

        // Then
        assertEquals(IL_1, optimizedExpression)
    }

    @Test
    fun shouldReplaceAddIntegerLiteralsInTreeWithOneLiteral() {
        // Given
        val addExpression1 = AddExpression(0, 0, IL_2, IL_1)
        val addExpression2 = AddExpression(0, 0, IDE_I64_A, addExpression1)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression2)

        // Then
        val expectedExpression = AddExpression(0, 0, IDE_I64_A, IL_3)
        assertEquals(expectedExpression, optimizedExpression)
    }

    @Test
    fun shouldOptimizeListOfIntegerAdditions() {
        // Given
        val addExpression1 = AddExpression(0, 0, IL_2, IL_1)
        val addExpression2 = AddExpression(0, 0, IL_0, IL_6)
        val addExpression3 = AddExpression(0, 0, IL_0, IL_0)

        // When
        val optimizedExpressions = expressionOptimizer.expressions(listOf(addExpression1, addExpression2, addExpression3))

        // Then
        val expectedExpressions = listOf(IL_3, IL_6, IL_0)
        assertEquals(expectedExpressions, optimizedExpressions)
    }

    @Test
    fun shouldReplaceAddStringLiteralsWithOneLiteral() {
        // Given
        val addExpression = AddExpression(0, 0, SL_ONE, SL_TWO)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression)

        // Then
        assertEquals(SL_ONE_TWO, optimizedExpression)
    }

    @Test
    fun shouldReplaceAddFloatLiteralsWithOneLiteral() {
        // Given
        val addExpression = AddExpression(0, 0, FL_1_00, FL_3_14)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression)

        // Then
        assertEquals(FloatLiteral::class.java, optimizedExpression.javaClass)
        assertEquals(FL_4_14.asDouble(), (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldReplaceAddFloatAndIntegerWithOneLiteral() {
        // Given
        val addExpression = AddExpression(0, 0, IL_1, FL_3_14)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression)

        // Then
        assertEquals(FloatLiteral::class.java, optimizedExpression.javaClass)
        assertEquals(FL_4_14.asDouble(), (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldNotReplaceAddLiteralAndIdent() {
        // Given
        val addExpression = AddExpression(0, 0, IL_1, IDE_I64_A)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression)

        // Then
        assertEquals(addExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceSubIntegerLiteralsWithOneLiteral() {
        // Given
        val subExpression = SubExpression(0, 0, IL_3, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(subExpression)

        // Then
        assertEquals(IL_2, optimizedExpression)
    }

    @Test
    fun shouldReplaceSubZeroWithOneLiteral() {
        // Given
        val subExpression = SubExpression(0, 0, IL_0, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(subExpression)

        // Then
        assertEquals(IL_1_NEG, optimizedExpression)
    }

    @Test
    fun shouldReplaceSubIntegerLiteralsInTreeWithOneLiteral() {
        // Given
        val subExpression1 = SubExpression(0, 0, IL_2, IL_1)
        val subExpression2 = SubExpression(0, 0, IDE_I64_A, subExpression1)

        // When
        val optimizedExpression = expressionOptimizer.expression(subExpression2)

        // Then
        val expectedExpression = SubExpression(0, 0, IDE_I64_A, IL_1)
        assertEquals(expectedExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceSubFloatAndIntegerWithOneLiteral() {
        // Given
        val subExpression = SubExpression(0, 0, IL_1, FL_3_14)

        // When
        val optimizedExpression = expressionOptimizer.expression(subExpression)

        // Then
        assertEquals(FloatLiteral::class.java, optimizedExpression.javaClass)
        assertEquals(-2.14, (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldNotReplaceSubLiteralAndFunctionCall() {
        // Given
        val subExpression = SubExpression(0, 0, IL_1, FC_FOO)

        // When
        val optimizedExpression = expressionOptimizer.expression(subExpression)

        // Then
        assertEquals(subExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceMulZeroWithZero() {
        // Given
        val mulExpression = MulExpression(0, 0, IL_0, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression)

        // Then
        assertEquals(IL_0, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceMulZeroAndFunctionCall() {
        // Given
        val mulExpression = MulExpression(0, 0, IL_0, FC_FOO)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression)

        // Then
        assertEquals(mulExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceMulOneWithOneLiteral() {
        // Given
        val mulExpression = MulExpression(0, 0, IL_1, IDE_I64_A)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression)

        // Then
        assertEquals(IDE_I64_A, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceMulLiteralAndIdent() {
        // Given
        val mulExpression = MulExpression(0, 0, IDE_I64_A, IL_3)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression)

        // Then
        assertEquals(mulExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceMulIntegerLiteralsWithOneLiteral() {
        // Given
        val mulExpression = MulExpression(0, 0, IL_2, IL_3)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression)

        // Then
        assertEquals(IL_6, optimizedExpression)
    }

    @Test
    fun shouldReplaceMulFloatLiteralsWithOneLiteral() {
        // Given
        val mulExpression = MulExpression(0, 0, FL_2_25, FL_3_14)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression)

        // Then
        assertEquals(7.065, (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldReplaceMulFloatAndIntegerWithOneLiteral() {
        // Given
        val mulExpression = MulExpression(0, 0, FL_2_25, IL_2)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression)

        // Then
        assertEquals(4.50, (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldReplaceMulPowerOfTwoWithShift() {
        // Given
        val addExpression = AddExpression(0, 0, IL_2, IL_2)
        val mulExpression = MulExpression(0, 0, IDE_I64_A, addExpression)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression)

        // Then
        // Multiplying with 4 equals shifting 2 bits left
        val expectedExpression = ShiftLeftExpression(0, 0, IDE_I64_A, IL_2)
        assertEquals(expectedExpression, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceNormalMultiplicationWithShift() {
        // Given
        val mulExpression = MulExpression(0, 0, IDE_I64_A, IL_3)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression)

        // Then
        assertEquals(mulExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceDivZeroWithZero() {
        // Given
        val divExpression = DivExpression(0, 0, IL_0, FL_1_00)

        // When
        val optimizedExpression = expressionOptimizer.expression(divExpression)

        // Then
        assertEquals(FL_0_00.asDouble(), (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldReplaceDivByOneWithOneLiteral() {
        // Given
        val divExpression = DivExpression(0, 0, FL_3_14, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(divExpression)

        // Then
        assertEquals(FL_3_14, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceDivZeroByFunctionCall() {
        // Given
        val divExpression = DivExpression(0, 0, IL_0, FC_FOO)

        // When
        val optimizedExpression = expressionOptimizer.expression(divExpression)

        // Then
        assertEquals(divExpression, optimizedExpression)
    }

    @Test(expected = InvalidException::class)
    fun shouldFailOnDivByZero() {
        // Given
        val divExpression = DivExpression(0, 0, IL_1, IL_0)

        // When
        expressionOptimizer.expression(divExpression)
    }

    @Test
    fun shouldReplaceDivFloatAndIntegerWithOneLiteral() {
        // Given
        val divExpression = DivExpression(0, 0, FL_1_00, IL_2)

        // When
        val optimizedExpression = expressionOptimizer.expression(divExpression)

        // Then
        assertEquals(0.5, (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldReplaceIDivZeroWithZero() {
        // Given
        val iDivExpression = IDivExpression(0, 0, IL_0, IL_3)

        // When
        val optimizedExpression = expressionOptimizer.expression(iDivExpression)

        // Then
        assertEquals(IL_0, optimizedExpression)
    }

    @Test
    fun shouldReplaceIDivByOneWithOneLiteral() {
        // Given
        val iDivExpression = IDivExpression(0, 0, IL_3, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(iDivExpression)

        // Then
        assertEquals(IL_3, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceIDivZeroByFunctionCall() {
        // Given
        val iDivExpression = IDivExpression(0, 0, IL_0, FC_FOO)

        // When
        val optimizedExpression = expressionOptimizer.expression(iDivExpression)

        // Then
        assertEquals(iDivExpression, optimizedExpression)
    }

    @Test(expected = InvalidException::class)
    fun shouldFailOnIDivByZero() {
        // Given
        val iDivExpression = IDivExpression(0, 0, IL_1, IL_0)

        // When
        expressionOptimizer.expression(iDivExpression)
    }

    @Test
    fun shouldReplaceIDivIntegerLiteralsWithOneLiteral() {
        // Given
        val iDivExpression = IDivExpression(0, 0, IL_6, IL_3)

        // When
        val optimizedExpression = expressionOptimizer.expression(iDivExpression)

        // Then
        assertEquals(IL_2, optimizedExpression)
    }

    companion object {
        private val FL_0_00 = FloatLiteral(0, 0, "0.00")
        private val FL_1_00 = FloatLiteral(0, 0, "1.00")
        private val FL_2_25 = FloatLiteral(0, 0, "2.25")
        private val FL_3_14 = FloatLiteral(0, 0, "3.14")
        private val FL_4_14 = FloatLiteral(0, 0, "4.14")

        private val IL_0 = IntegerLiteral(0, 0, "0")
        private val IL_1 = IntegerLiteral(0, 0, "1")
        private val IL_2 = IntegerLiteral(0, 0, "2")
        private val IL_3 = IntegerLiteral(0, 0, "3")
        private val IL_6 = IntegerLiteral(0, 0, "6")
        private val IL_1_NEG = IntegerLiteral(0, 0, "-1")

        private val SL_ONE = StringLiteral(0, 0, "One")
        private val SL_TWO = StringLiteral(0, 0, "Two")
        private val SL_ONE_TWO = StringLiteral(0, 0, "OneTwo")

        private val IDENT_I64_A = Identifier("a%", I64.INSTANCE)
        private val IDE_I64_A = IdentifierDerefExpression(0, 0, IDENT_I64_A)

        private val FC_FOO = FunctionCallExpression(0, 0, Identifier("foo", I64.INSTANCE), listOf(IL_1))

        // We have to use the default type manager here, since we don't have access to any other.
        // If this becomes a problem for the tests, we will have to make the default type manager
        // more advanced.
        private val expressionOptimizer = DefaultAstExpressionOptimizer(DefaultTypeManager())
    }
}
