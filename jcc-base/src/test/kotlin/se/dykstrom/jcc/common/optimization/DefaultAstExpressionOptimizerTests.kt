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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.common.BaseTests.Companion.FCE_FOO
import se.dykstrom.jcc.common.BaseTests.Companion.FL_0_0
import se.dykstrom.jcc.common.BaseTests.Companion.FL_1_0
import se.dykstrom.jcc.common.BaseTests.Companion.FL_2_25
import se.dykstrom.jcc.common.BaseTests.Companion.FL_3_14
import se.dykstrom.jcc.common.BaseTests.Companion.FL_4_14
import se.dykstrom.jcc.common.BaseTests.Companion.FL_M1_0
import se.dykstrom.jcc.common.BaseTests.Companion.FL_M3_14
import se.dykstrom.jcc.common.BaseTests.Companion.IDENT_I64_A
import se.dykstrom.jcc.common.BaseTests.Companion.IDENT_STR_S
import se.dykstrom.jcc.common.BaseTests.Companion.IDE_F64_F
import se.dykstrom.jcc.common.BaseTests.Companion.IDE_I64_A
import se.dykstrom.jcc.common.BaseTests.Companion.IDE_STR_S
import se.dykstrom.jcc.common.BaseTests.Companion.IL_0
import se.dykstrom.jcc.common.BaseTests.Companion.IL_1
import se.dykstrom.jcc.common.BaseTests.Companion.IL_2
import se.dykstrom.jcc.common.BaseTests.Companion.IL_3
import se.dykstrom.jcc.common.BaseTests.Companion.IL_6
import se.dykstrom.jcc.common.BaseTests.Companion.IL_M1
import se.dykstrom.jcc.common.BaseTests.Companion.SL_ONE
import se.dykstrom.jcc.common.BaseTests.Companion.SL_ONE_TWO
import se.dykstrom.jcc.common.BaseTests.Companion.SL_TWO
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.ast.FloatLiteral.FL_F32_0_0
import se.dykstrom.jcc.common.ast.IntegerLiteral.*
import se.dykstrom.jcc.common.compiler.AbstractTypeManager
import se.dykstrom.jcc.common.error.InvalidValueException
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.*

/**
 * Tests class `DefaultAstExpressionOptimizer`.
 *
 * @author Johan Dykstrom
 * @see DefaultAstExpressionOptimizer
 */
class DefaultAstExpressionOptimizerTests {

    private val symbolTable = SymbolTable()

    @Test
    fun shouldReplaceAddIntegerLiteralsWithOneLiteral() {
        // Given
        val addExpression = AddExpression(IL_2, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression, symbolTable)

        // Then
        assertEquals(IL_3, optimizedExpression)
    }

    @Test
    fun shouldOptimizeInsideUnaryExpressionsWithExtraFields() {
        // These keep a field that UnaryExpression's reflective withExpression cannot copy
        val function = LibraryFunction("f", listOf(F64.INSTANCE), F64.INSTANCE, ExternalFunction("f"))
        val add = AddExpression(IL_2, IL_1)

        assertEquals(RoundExpression(IL_3, function), expressionOptimizer.expression(RoundExpression(add, function), symbolTable))
        assertEquals(AbsExpression(IL_3, function), expressionOptimizer.expression(AbsExpression(add, function), symbolTable))
    }

    @Test
    fun shouldReplaceAddZeroAndLiteralWithOneLiteral() {
        // Given
        val addExpression = AddExpression(IL_0, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression, symbolTable)

        // Then
        assertEquals(IL_1, optimizedExpression)
    }

    @Test
    fun shouldReplaceAddZeroAndIdeWithOneIde() {
        // Given
        val addExpression = AddExpression(IDE_I64_A, IL_0)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression, symbolTable)

        // Then
        assertEquals(IDE_I64_A, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceAddFloatZeroAndIde() {
        // Folding 0.0 + x to x would violate IEEE 754: the result is +0.0 when x is -0.0
        // Given
        val addExpression = AddExpression(FL_0_0, IDE_F64_F)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression, symbolTable)

        // Then
        assertEquals(addExpression, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceAddFloatIdeAndZero() {
        // Given
        val addExpression = AddExpression(IDE_F64_F, FL_0_0)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression, symbolTable)

        // Then
        assertEquals(addExpression, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceAddIntegerZeroAndFloatIde() {
        // Given
        val addExpression = AddExpression(IL_0, IDE_F64_F)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression, symbolTable)

        // Then
        assertEquals(addExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceAddIntegerLiteralsInTreeWithOneLiteral() {
        // Given
        val addExpression1 = AddExpression(IL_2, IL_1)
        val addExpression2 = AddExpression(IDE_I64_A, addExpression1)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression2, symbolTable)

        // Then
        val expectedExpression = AddExpression(IDE_I64_A, IL_3)
        assertEquals(expectedExpression, optimizedExpression)
    }

    @Test
    fun shouldOptimizeListOfIntegerAdditions() {
        // Given
        val addExpression1 = AddExpression(IL_2, IL_1)
        val addExpression2 = AddExpression(IL_0, IL_6)
        val addExpression3 = AddExpression(IL_0, IL_0)

        // When
        val optimizedExpressions = expressionOptimizer.expressions(
            listOf(addExpression1, addExpression2, addExpression3),
            SymbolTable()
        )

        // Then
        val expectedExpressions = listOf(IL_3, IL_6, IL_0)
        assertEquals(expectedExpressions, optimizedExpressions)
    }

    @Test
    fun shouldReplaceNegNegExprWithJustExpr() {
        // Given
        val negNegExpression = NegateExpression(NegateExpression(IDE_I64_A))

        // When
        val optimizedExpression = expressionOptimizer.expression(negNegExpression, symbolTable)

        // Then
        assertEquals(IDE_I64_A, optimizedExpression)
    }

    @Test
    fun shouldReplaceAddStringLiteralsWithOneLiteral() {
        // Given
        val addExpression = AddExpression(SL_ONE, SL_TWO)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression, symbolTable)

        // Then
        assertEquals(SL_ONE_TWO, optimizedExpression)
    }

    @Test
    fun shouldReplaceAddFloatLiteralsWithOneLiteral() {
        // Given
        val addExpression = AddExpression(FL_1_0, FL_3_14)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression, symbolTable)

        // Then
        assertEquals(FloatLiteral::class.java, optimizedExpression.javaClass)
        assertEquals(FL_4_14.asDouble(), (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldFoldF32LiteralsToF32Literal() {
        val a = FloatLiteral(0, 0, "0.1", F32.INSTANCE)
        val b = FloatLiteral(0, 0, "0.2", F32.INSTANCE)
        assertF32(0.1f + 0.2f, AddExpression(a, b))
        assertF32(0.1f - 0.2f, SubExpression(a, b))
        assertF32(0.1f * 0.2f, MulExpression(a, b))
        assertF32(0.1f / 0.2f, DivExpression(a, b))
        assertF32(-0.1f, NegateExpression(a))
    }

    @Test
    fun shouldFoldF32AndF64LiteralsToF64Literal() {
        val a = FloatLiteral(0, 0, "0.5", F32.INSTANCE)
        val optimizedExpression = expressionOptimizer.expression(AddExpression(a, FL_1_0), symbolTable)
        assertEquals(F64.INSTANCE, (optimizedExpression as FloatLiteral).type())
        assertEquals(1.5, optimizedExpression.asDouble())
    }

    @Test
    fun shouldNotFoldOverflowingF32Division() {
        val big = FloatLiteral(0, 0, "3.0E38", F32.INSTANCE)
        val small = FloatLiteral(0, 0, "0.001", F32.INSTANCE)
        val divExpression = DivExpression(big, small)
        assertEquals(divExpression, expressionOptimizer.expression(divExpression, symbolTable))
    }

    @Test
    fun shouldFoldI32LiteralsToI32Literal() {
        val max = IntegerLiteral(0, 0, Int.MAX_VALUE.toString(), I32.INSTANCE)
        val seven = IntegerLiteral(0, 0, "7", I32.INSTANCE)
        val two = IntegerLiteral(0, 0, "2", I32.INSTANCE)
        // Wraps around at 32 bits, as at runtime
        assertI32(Int.MIN_VALUE, AddExpression(max, ONE_I32))
        assertI32(Int.MAX_VALUE, SubExpression(IntegerLiteral(0, 0, Int.MIN_VALUE.toString(), I32.INSTANCE), ONE_I32))
        assertI32(-2, MulExpression(max, two))
        assertI32(3, IDivExpression(seven, two))
        assertI32(1, ModExpression(seven, two))
        assertI32(2, AndExpression(seven, two))
        assertI32(7, OrExpression(seven, two))
        assertI32(5, XorExpression(seven, two))
        assertI32(-7, NegateExpression(seven))
        assertI32(-8, NotExpression(seven))
    }

    @Test
    fun shouldFoldI64LiteralsWithoutWrappingAt32Bits() {
        val max = IntegerLiteral(0, 0, Int.MAX_VALUE.toString(), I64.INSTANCE)
        val optimizedExpression = expressionOptimizer.expression(AddExpression(max, IL_1), symbolTable)
        assertEquals(IntegerLiteral(0, 0, "2147483648", I64.INSTANCE), optimizedExpression)
    }

    @Test
    fun shouldNotFoldOverflowingIntegerDivision() {
        // The smallest value divided by -1 is undefined at runtime
        val minI32 = IntegerLiteral(0, 0, Int.MIN_VALUE.toString(), I32.INSTANCE)
        val minusOneI32 = IntegerLiteral(0, 0, "-1", I32.INSTANCE)
        val minI64 = IntegerLiteral(0, 0, Long.MIN_VALUE.toString(), I64.INSTANCE)
        listOf(
            IDivExpression(minI32, minusOneI32),
            ModExpression(minI32, minusOneI32),
            IDivExpression(minI64, M_ONE),
            ModExpression(minI64, M_ONE),
        ).forEach { assertEquals(it, expressionOptimizer.expression(it, symbolTable)) }
    }

    @Test
    fun shouldFoldDivisionOfI32MinAsI64ByMinusOne() {
        val minI32AsI64 = IntegerLiteral(0, 0, Int.MIN_VALUE.toString(), I64.INSTANCE)
        val optimizedExpression = expressionOptimizer.expression(IDivExpression(minI32AsI64, M_ONE), symbolTable)
        assertEquals(IntegerLiteral(0, 0, "2147483648", I64.INSTANCE), optimizedExpression)
    }

    @Test
    fun shouldShiftI32ByI32Amount() {
        val ideI32 = IdentifierDerefExpression(Identifier("i", I32.INSTANCE))
        val four = IntegerLiteral(0, 0, "4", I32.INSTANCE)
        val expected = ShiftLeftExpression(ideI32, IntegerLiteral(0, 0, "2", I32.INSTANCE))
        assertEquals(expected, expressionOptimizer.expression(MulExpression(ideI32, four), symbolTable))
        assertEquals(I32.INSTANCE, typeManager.getType(expected))
    }

    @Test
    fun shouldFoldToZeroOfExpressionType() {
        val ideI32 = IdentifierDerefExpression(Identifier("i", I32.INSTANCE))
        assertI32(0, ModExpression(ideI32, ONE_I32))
        assertI32(0, IDivExpression(ZERO_I32, ideI32))
        assertI32(0, AndExpression(ideI32, ZERO_I32))
    }

    @Test
    fun shouldFoldComparisonToBooleanLiteralWhenTypedAsBool() {
        assertEquals(BooleanLiteral.TRUE, boolOptimizer.expression(LessExpression(IL_1, IL_2), symbolTable))
        assertEquals(BooleanLiteral.FALSE, boolOptimizer.expression(GreaterExpression(IL_1, IL_2), symbolTable))
        assertEquals(BooleanLiteral.TRUE, boolOptimizer.expression(NotEqualExpression(FL_1_0, FL_3_14), symbolTable))
    }

    @Test
    fun shouldFoldComparisonToIntegerLiteralOtherwise() {
        assertEquals(M_ONE, expressionOptimizer.expression(LessExpression(IL_1, IL_2), symbolTable))
        assertEquals(ZERO, expressionOptimizer.expression(GreaterExpression(IL_1, IL_2), symbolTable))
    }

    @Test
    fun shouldFoldFloatComparisonFollowingIeee754() {
        val minusZero = FloatLiteral(0, 0, "-0.0")
        val zero = FloatLiteral(0, 0, "0.0")
        assertEquals(M_ONE, expressionOptimizer.expression(EqualExpression(minusZero, zero), symbolTable))
        assertEquals(ZERO, expressionOptimizer.expression(LessExpression(minusZero, zero), symbolTable))
    }

    private fun assertI32(expected: Int, expression: Expression) {
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable) as IntegerLiteral
        assertEquals(I32.INSTANCE, optimizedExpression.type())
        assertEquals(expected.toLong(), optimizedExpression.asLong())
    }

    private fun assertF32(expected: Float, expression: Expression) {
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable) as FloatLiteral
        assertEquals(F32.INSTANCE, optimizedExpression.type())
        assertEquals(expected, optimizedExpression.asDouble().toFloat())
        assertEquals(expected.toDouble(), optimizedExpression.asDouble())
    }

    @Test
    fun shouldReplaceAddFloatAndIntegerWithOneLiteral() {
        // Given
        val addExpression = AddExpression(IL_1, FL_3_14)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression, symbolTable)

        // Then
        assertEquals(FloatLiteral::class.java, optimizedExpression.javaClass)
        assertEquals(FL_4_14.asDouble(), (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldNotReplaceAddLiteralAndIdent() {
        // Given
        val addExpression = AddExpression(IL_1, IDE_I64_A)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression, symbolTable)

        // Then
        assertEquals(addExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceSubIntegerLiteralsWithOneLiteral() {
        // Given
        val subExpression = SubExpression(IL_3, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(subExpression, symbolTable)

        // Then
        assertEquals(IL_2, optimizedExpression)
    }

    @Test
    fun shouldReplaceSubZeroWithOneLiteral() {
        // Given
        val subExpression = SubExpression(IL_0, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(subExpression, symbolTable)

        // Then
        assertEquals(IL_M1, optimizedExpression)
    }

    @Test
    fun shouldReplaceSubIntegerLiteralsInTreeWithOneLiteral() {
        // Given
        val subExpression1 = SubExpression(IL_2, IL_1)
        val subExpression2 = SubExpression(IDE_I64_A, subExpression1)

        // When
        val optimizedExpression = expressionOptimizer.expression(subExpression2, symbolTable)

        // Then
        val expectedExpression = SubExpression(IDE_I64_A, IL_1)
        assertEquals(expectedExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceSubFloatAndIntegerWithOneLiteral() {
        // Given
        val subExpression = SubExpression(IL_1, FL_3_14)

        // When
        val optimizedExpression = expressionOptimizer.expression(subExpression, symbolTable)

        // Then
        assertEquals(FloatLiteral::class.java, optimizedExpression.javaClass)
        assertEquals(-2.14, (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldReplaceSubFloatIdeAndZeroWithIde() {
        // Subtracting zero is an IEEE 754 identity, so the fold is valid also for floats
        // Given
        val subExpression = SubExpression(IDE_F64_F, FL_0_0)

        // When
        val optimizedExpression = expressionOptimizer.expression(subExpression, symbolTable)

        // Then
        assertEquals(IDE_F64_F, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceSubIntegerIdeAndFloatZero() {
        // The expression is float-typed, so folding it to the integer identifier would change its type
        // Given
        val subExpression = SubExpression(IDE_I64_A, FL_0_0)

        // When
        val optimizedExpression = expressionOptimizer.expression(subExpression, symbolTable)

        // Then
        assertEquals(subExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceSubFloatZeroAndIntegerWithFloatLiteral() {
        // Given
        val subExpression = SubExpression(FL_0_0, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(subExpression, symbolTable)

        // Then
        assertEquals(FloatLiteral::class.java, optimizedExpression.javaClass)
        assertEquals(-1.0, (optimizedExpression as FloatLiteral).asDouble())
    }

    @Test
    fun shouldNotReplaceSubLiteralAndFunctionCall() {
        // Given
        val subExpression = SubExpression(IL_1, FCE_FOO)

        // When
        val optimizedExpression = expressionOptimizer.expression(subExpression, symbolTable)

        // Then
        assertEquals(subExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceMulZeroWithZero() {
        // Given
        val mulExpression = MulExpression(IL_0, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(IL_0, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceMulZeroAndFunctionCall() {
        // Given
        val mulExpression = MulExpression(IL_0, FCE_FOO)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(mulExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceMulOneWithOneLiteral() {
        // Given
        val mulExpression = MulExpression(IL_1, IDE_I64_A)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(IDE_I64_A, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceMulFloatZeroAndIde() {
        // Folding 0.0 * x to 0.0 would violate IEEE 754: 0.0 * inf is NaN and 0.0 * -1.0 is -0.0
        // Given
        val mulExpression = MulExpression(FL_0_0, IDE_F64_F)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(mulExpression, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceMulIntegerZeroAndFloatIde() {
        // Given
        val mulExpression = MulExpression(IL_0, IDE_F64_F)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(mulExpression, optimizedExpression)
    }

    @Test
    fun shouldPreserveSignWhenFoldingMulFloatZeroLiterals() {
        // Given
        val mulExpression = MulExpression(FL_0_0, FL_M1_0)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(FloatLiteral::class.java, optimizedExpression.javaClass)
        assertEquals(-0.0, (optimizedExpression as FloatLiteral).asDouble())
    }

    @Test
    fun shouldReplaceMulOneAndFloatIdeWithIde() {
        // Given
        val mulExpression = MulExpression(IL_1, IDE_F64_F)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(IDE_F64_F, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceMulFloatOneAndIntegerIde() {
        // The expression is float-typed, so folding it to the integer identifier would change its type
        // Given
        val mulExpression = MulExpression(FL_1_0, IDE_I64_A)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(mulExpression, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceMulLiteralAndIdent() {
        // Given
        val mulExpression = MulExpression(IDE_I64_A, IL_3)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(mulExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceMulIntegerLiteralsWithOneLiteral() {
        // Given
        val mulExpression = MulExpression(IL_2, IL_3)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(IL_6, optimizedExpression)
    }

    @Test
    fun shouldReplaceMulFloatLiteralsWithOneLiteral() {
        // Given
        val mulExpression = MulExpression(FL_2_25, FL_3_14)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(7.065, (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldReplaceMulFloatAndIntegerWithOneLiteral() {
        // Given
        val mulExpression = MulExpression(FL_2_25, IL_2)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(4.50, (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldReplaceMulPowerOfTwoWithShift() {
        // Given
        val addExpression = AddExpression(IL_2, IL_2)
        val mulExpression = MulExpression(IDE_I64_A, addExpression)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        // Multiplying with 4 equals shifting 2 bits left
        val expectedExpression = ShiftLeftExpression(IDE_I64_A, IL_2)
        assertEquals(expectedExpression, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceNormalMultiplicationWithShift() {
        // Given
        val mulExpression = MulExpression(IDE_I64_A, IL_3)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(mulExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceDivZeroByLiteralWithZero() {
        // Given
        val divExpression = DivExpression(IL_0, FL_1_0)

        // When
        val optimizedExpression = expressionOptimizer.expression(divExpression, symbolTable)

        // Then
        assertEquals(FL_0_0.asDouble(), (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldNotReplaceDivZeroByIde() {
        // Folding 0.0 / x to 0.0 would violate IEEE 754 for negative or NaN x
        // Given
        val divExpression = DivExpression(IL_0, IDE_I64_A)

        // When
        val optimizedExpression = expressionOptimizer.expression(divExpression, symbolTable)

        // Then
        assertEquals(divExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceDivByOneWithOneLiteral() {
        // Given
        val divExpression = DivExpression(FL_3_14, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(divExpression, symbolTable)

        // Then
        assertEquals(FL_3_14, optimizedExpression)
    }

    @Test
    fun shouldReplaceDivFloatIdeByOneWithIde() {
        // Given
        val divExpression = DivExpression(IDE_F64_F, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(divExpression, symbolTable)

        // Then
        assertEquals(IDE_F64_F, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceDivIntegerIdeByFloatOne() {
        // Dividing an integer by a float gives a float, so folding it to the integer identifier would change its type
        // Given
        val divExpression = DivExpression(IDE_I64_A, FL_1_0)

        // When
        val optimizedExpression = expressionOptimizer.expression(divExpression, symbolTable)

        // Then
        assertEquals(divExpression, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceDivZeroByFunctionCall() {
        // Given
        val divExpression = DivExpression(IL_0, FCE_FOO)

        // When
        val optimizedExpression = expressionOptimizer.expression(divExpression, symbolTable)

        // Then
        assertEquals(divExpression, optimizedExpression)
    }

    @Test
    fun shouldFailOnDivByZero() {
        // Given
        val divExpression = DivExpression(IL_1, IL_0)

        // When & Then
        assertThrows<InvalidValueException> { expressionOptimizer.expression(divExpression, symbolTable) }
    }

    @Test
    fun shouldReplaceDivFloatAndIntegerWithOneLiteral() {
        // Given
        val divExpression = DivExpression(FL_1_0, IL_2)

        // When
        val optimizedExpression = expressionOptimizer.expression(divExpression, symbolTable)

        // Then
        assertEquals(0.5, (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldReplaceIDivZeroByLiteralWithZero() {
        // Given
        val iDivExpression = IDivExpression(IL_0, IL_3)

        // When
        val optimizedExpression = expressionOptimizer.expression(iDivExpression, symbolTable)

        // Then
        assertEquals(IL_0, optimizedExpression)
    }

    @Test
    fun shouldReplaceIDivZeroByIdeWithZero() {
        // Given
        val iDivExpression = IDivExpression(IL_0, IDE_I64_A)

        // When
        val optimizedExpression = expressionOptimizer.expression(iDivExpression, symbolTable)

        // Then
        assertEquals(IL_0, optimizedExpression)
    }

    @Test
    fun shouldReplaceIDivByOneWithLiteral() {
        // Given
        val iDivExpression = IDivExpression(IL_3, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(iDivExpression, symbolTable)

        // Then
        assertEquals(IL_3, optimizedExpression)
    }

    @Test
    fun shouldReplaceIDivByOneWithIde() {
        // Given
        val iDivExpression = IDivExpression(IDE_I64_A, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(iDivExpression, symbolTable)

        // Then
        assertEquals(IDE_I64_A, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceIDivZeroByFunctionCall() {
        // Given
        val iDivExpression = IDivExpression(IL_0, FCE_FOO)

        // When
        val optimizedExpression = expressionOptimizer.expression(iDivExpression, symbolTable)

        // Then
        assertEquals(iDivExpression, optimizedExpression)
    }

    @Test
    fun shouldFailOnIDivByZero() {
        // Given
        val iDivExpression = IDivExpression(IL_1, IL_0)

        // When & Then
        assertThrows<InvalidValueException> { expressionOptimizer.expression(iDivExpression, symbolTable) }
    }

    @Test
    fun shouldReplaceIDivIntegerLiteralsWithOneLiteral() {
        // Given
        val iDivExpression = IDivExpression(IL_6, IL_3)

        // When
        val optimizedExpression = expressionOptimizer.expression(iDivExpression, symbolTable)

        // Then
        assertEquals(IL_2, optimizedExpression)
    }

    @Test
    fun shouldReplaceModIntegerLiteralsWithOneLiteral() {
        // Given
        val modExpression = ModExpression(IL_3, IL_2)

        // When
        val optimizedExpression = expressionOptimizer.expression(modExpression, symbolTable)

        // Then
        assertEquals(IL_1, optimizedExpression)
    }

    @Test
    fun shouldReplaceModByOneWithZero() {
        // Given
        val modExpression = ModExpression(IL_3, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(modExpression, symbolTable)

        // Then
        assertEquals(IL_0, optimizedExpression)
    }

    @Test
    fun shouldReplaceModZeroByLiteralWithZero() {
        // Given
        val modExpression = ModExpression(IL_0, IL_3)

        // When
        val optimizedExpression = expressionOptimizer.expression(modExpression, symbolTable)

        // Then
        assertEquals(IL_0, optimizedExpression)
    }

    @Test
    fun shouldFailOnModByZero() {
        // Given
        val modExpression = ModExpression(IL_3, IL_0)

        // When & Then
        assertThrows<InvalidValueException> { expressionOptimizer.expression(modExpression, symbolTable) }
    }

    @Test
    fun shouldReplaceNegateIntegerLiteralWithOneLiteral() {
        // Given
        val negateExpression = NegateExpression(IL_M1)

        // When
        val optimizedExpression = expressionOptimizer.expression(negateExpression, symbolTable)

        // Then
        assertEquals(IL_1, optimizedExpression)
    }

    @Test
    fun shouldReplaceNegateFloatLiteralWithOneLiteral() {
        // Given
        val negateExpression = NegateExpression(FL_3_14)

        // When
        val optimizedExpression = expressionOptimizer.expression(negateExpression, symbolTable)

        // Then
        assertEquals(FL_M3_14, optimizedExpression)
    }

    @Test
    fun shouldReplaceNegateSimpleSubExpressionWithOneLiteral() {
        // Given
        val negateExpression = NegateExpression(SubExpression(IL_3, IL_2))

        // When
        val optimizedExpression = expressionOptimizer.expression(negateExpression, symbolTable)

        // Then
        assertEquals(IL_M1, optimizedExpression)
    }

    @Test
    fun shouldReplaceNegateComplexSubExpressionWithSubExpression() {
        // Given
        val negateExpression = NegateExpression(SubExpression(IDE_I64_A, IL_1)) // -(a - 1)
        val expectedExpression = SubExpression(IL_1, IDE_I64_A) // 1 - a

        // When
        val optimizedExpression = expressionOptimizer.expression(negateExpression, symbolTable)

        // Then
        assertEquals(expectedExpression, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceNegateSubExpressionThatHasFunctionCall() {
        // Given
        val negateExpression = NegateExpression(SubExpression(FCE_FOO, IL_1))

        // When
        val optimizedExpression = expressionOptimizer.expression(negateExpression, symbolTable)

        // Then
        assertEquals(negateExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceCastToIntegerWithTypedLiteral() {
        // From integer
        assertEquals(ONE_I32, expressionOptimizer.expression(CastToIntExpression(IL_1, I32.INSTANCE), symbolTable))
        assertEquals(ZERO, expressionOptimizer.expression(CastToIntExpression(ZERO_I32, I64.INSTANCE), symbolTable))

        // From float
        assertEquals(ONE_I32, expressionOptimizer.expression(CastToIntExpression(FL_1_0, I32.INSTANCE), symbolTable))
        assertEquals(IL_3, expressionOptimizer.expression(CastToIntExpression(FL_3_14, I64.INSTANCE), symbolTable))
    }

    @Test
    fun shouldReplaceCastToFloatWithTypedLiteral() {
        // From integer
        assertEquals(FL_1_0, expressionOptimizer.expression(CastToFloatExpression(IL_1, F64.INSTANCE), symbolTable))

        // From float
        assertEquals(FL_0_0, expressionOptimizer.expression(CastToFloatExpression(FL_F32_0_0, F64.INSTANCE), symbolTable))
    }

    @Test
    fun shouldReplaceDerefConstExpressionWithIntegerLiteral() {
        // Given
        val symbolTable = SymbolTable()
        symbolTable.addConstant(IDENT_I64_A, "6")

        // When
        val optimizedExpression = expressionOptimizer.expression(IDE_I64_A, symbolTable)

        // Then
        assertEquals(IL_6, optimizedExpression)
    }

    @Test
    fun shouldReplaceDerefConstExpressionWithStringLiteral() {
        // Given
        val symbolTable = SymbolTable()
        symbolTable.addConstant(IDENT_STR_S, "Two")

        // When
        val addExpression = AddExpression(SL_ONE, IDE_STR_S)
        val optimizedExpression = expressionOptimizer.expression(addExpression, symbolTable)

        // Then
        assertEquals(SL_ONE_TWO, optimizedExpression)
    }

    @Test
    fun shouldOptimizeIfExpressionChildren() {
        // Given
        val ide = IdentifierDerefExpression(Identifier("b", Bool.INSTANCE))
        val expression = IfExpression(ide, AddExpression(IL_1, IL_2), AddExpression(IL_2, IL_1))

        // When
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable)

        // Then
        assertEquals(IfExpression(ide, IL_3, IL_3), optimizedExpression)
    }

    @Test
    fun shouldReplaceIfExpressionWithSelectedBranch() {
        val thenExpr = AddExpression(IDE_I64_A, IL_1)
        val elseExpr = AddExpression(IDE_I64_A, IL_2)
        val condition = LessExpression(IL_1, IL_2)

        assertEquals(thenExpr, boolOptimizer.expression(IfExpression(BooleanLiteral.TRUE, thenExpr, elseExpr), symbolTable))
        assertEquals(elseExpr, boolOptimizer.expression(IfExpression(BooleanLiteral.FALSE, thenExpr, elseExpr), symbolTable))
        // A condition that folds to a boolean literal selects its branch too
        assertEquals(thenExpr, boolOptimizer.expression(IfExpression(condition, thenExpr, elseExpr), symbolTable))
    }

    @Test
    fun shouldNotReplaceArrayAccessNamedLikeConstant() {
        // Given
        val symbolTable = SymbolTable()
        symbolTable.addConstant(IDENT_I64_A, "6")
        val arrayAccess = ArrayAccessExpression(Identifier(IDENT_I64_A.name(), Arr.from(1, I64.INSTANCE)), listOf(IL_1))

        // When
        val optimizedExpression = expressionOptimizer.expression(arrayAccess, symbolTable)

        // Then
        assertEquals(arrayAccess, optimizedExpression)
    }

    @Test
    fun shouldReplaceBuiltInFold() {
        // Given
        val symbolTable = SymbolTable()
        symbolTable.addConstant(IDENT_I64_A, "6")
        val components = listOf(DefaultAstExpressionOptimizer.ExpressionComponent(IdentifierDerefExpression::class.java) { e, _, _ -> e })
        val optimizer = DefaultAstExpressionOptimizer(typeManager, components)

        // When
        val optimizedExpression = optimizer.expression(AddExpression(IDE_I64_A, IL_2), symbolTable)

        // Then
        assertEquals(AddExpression(IDE_I64_A, IL_2), optimizedExpression)
    }

    @Test
    fun shouldAddFoldAfterOptimizingSubexpressions() {
        // Given
        val components = listOf(DefaultAstExpressionOptimizer.ExpressionComponent(LogicalAndExpression::class.java) { e, _, _ ->
            if (e.left == BooleanLiteral.TRUE) e.right else e
        })
        val optimizer = DefaultAstExpressionOptimizer(typeManager, components)
        val expression = LogicalAndExpression(BooleanLiteral.TRUE, AddExpression(IL_1, IL_2))

        // When
        val optimizedExpression = optimizer.expression(expression, symbolTable)

        // Then
        assertEquals(IL_3, optimizedExpression)
    }

    @Test
    fun shouldReplaceAndWithZeroWithZero() {
        // Given
        val expression = AndExpression(IDE_I64_A, IL_0)

        // When
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable)

        // Then
        assertEquals(IL_0, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceAndZeroWithFunctionCall() {
        // Given
        val expression = AndExpression(FCE_FOO, IL_0)

        // When
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable)

        // Then
        assertEquals(expression, optimizedExpression)
    }

    @Test
    fun shouldReplaceAndIntegerLiteralsWithOneLiteral() {
        // Given
        val expression = AndExpression(IL_2, IL_3)

        // When
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable)

        // Then
        assertEquals(IL_2, optimizedExpression)
    }

    @Test
    fun shouldReplaceOrExprWithZeroWithExpr() {
        // Given
        val expression = OrExpression(IDE_I64_A, IL_0)

        // When
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable)

        // Then
        assertEquals(IDE_I64_A, optimizedExpression)
    }

    @Test
    fun shouldReplaceOrIntegerLiteralsWithOneLiteral() {
        // Given
        val expression = OrExpression(IL_2, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable)

        // Then
        assertEquals(IL_3, optimizedExpression)
    }

    @Test
    fun shouldReplaceXorExprWithZeroWithExpr() {
        // Given
        val expression = XorExpression(IL_0, IL_2)

        // When
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable)

        // Then
        assertEquals(IL_2, optimizedExpression)
    }

    @Test
    fun shouldReplaceXorIntegerLiteralsWithOneLiteral() {
        // Given
        val expression = XorExpression(IL_3, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable)

        // Then
        assertEquals(IL_2, optimizedExpression)
    }

    @Test
    fun shouldReplaceNotIntegerLiteralWithOneLiteral() {
        // Given
        val expression = NotExpression(IL_M1)

        // When
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable)

        // Then
        assertEquals(IL_0, optimizedExpression)
    }

    @Test
    fun shouldReplaceNotNotExprWithJustExpr() {
        // Given
        val expression = NotExpression(NotExpression(IDE_I64_A))

        // When
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable)

        // Then
        assertEquals(IDE_I64_A, optimizedExpression)
    }

    @Test
    fun shouldReplaceEQIntegerLiteralsWithOneLiteral() {
        assertEquals(IL_0, expressionOptimizer.expression(EqualExpression(IL_3, IL_1), symbolTable))
        assertEquals(IL_M1, expressionOptimizer.expression(EqualExpression(IL_1, IL_1), symbolTable))
        assertEquals(IL_0, expressionOptimizer.expression(EqualExpression(IL_1, IL_3), symbolTable))
    }

    @Test
    fun shouldReplaceEQNumericLiteralsWithOneLiteral() {
        assertEquals(IL_0, expressionOptimizer.expression(EqualExpression(IL_6, FL_1_0), symbolTable))
        assertEquals(IL_M1, expressionOptimizer.expression(EqualExpression(IL_1, FL_1_0), symbolTable))
        assertEquals(IL_0, expressionOptimizer.expression(EqualExpression(IL_M1, FL_1_0), symbolTable))
    }

    @Test
    fun shouldReplaceNEIntegerLiteralsWithOneLiteral() {
        assertEquals(IL_M1, expressionOptimizer.expression(NotEqualExpression(IL_3, IL_1), symbolTable))
    }

    @Test
    fun shouldReplaceNEFloatLiteralsWithOneLiteral() {
        assertEquals(IL_M1, expressionOptimizer.expression(NotEqualExpression(FL_4_14, FL_3_14), symbolTable))
        assertEquals(IL_0, expressionOptimizer.expression(NotEqualExpression(FL_3_14, FL_3_14), symbolTable))
        assertEquals(IL_M1, expressionOptimizer.expression(NotEqualExpression(FL_3_14, FL_4_14), symbolTable))
    }

    @Test
    fun shouldReplaceGTIntegerLiteralsWithOneLiteral() {
        assertEquals(IL_M1, expressionOptimizer.expression(GreaterExpression(IL_3, IL_1), symbolTable))
    }

    @Test
    fun shouldReplaceLTIntegerLiteralsWithOneLiteral() {
        assertEquals(IL_0, expressionOptimizer.expression(LessExpression(IL_3, IL_1), symbolTable))
        assertEquals(IL_0, expressionOptimizer.expression(LessExpression(IL_1, IL_1), symbolTable))
        assertEquals(IL_M1, expressionOptimizer.expression(LessExpression(IL_1, IL_3), symbolTable))
    }

    @Test
    fun shouldReplaceLTFloatLiteralsWithOneLiteral() {
        assertEquals(IL_0, expressionOptimizer.expression(LessExpression(FL_4_14, FL_3_14), symbolTable))
        assertEquals(IL_0, expressionOptimizer.expression(LessExpression(FL_4_14, FL_4_14), symbolTable))
        assertEquals(IL_M1, expressionOptimizer.expression(LessExpression(FL_3_14, FL_4_14), symbolTable))
    }

    @Test
    fun shouldReplaceLTNumericLiteralsWithOneLiteral() {
        assertEquals(IL_0, expressionOptimizer.expression(LessExpression(IL_6, FL_1_0), symbolTable))
        assertEquals(IL_0, expressionOptimizer.expression(LessExpression(IL_1, FL_1_0), symbolTable))
        assertEquals(IL_M1, expressionOptimizer.expression(LessExpression(IL_M1, FL_1_0), symbolTable))
    }

    @Test
    fun shouldReplaceLTStringLiteralsWithOneLiteral() {
        assertEquals(IL_0, expressionOptimizer.expression(LessExpression(SL_TWO, SL_ONE), symbolTable))
        assertEquals(IL_0, expressionOptimizer.expression(LessExpression(SL_ONE, SL_ONE), symbolTable))
        assertEquals(IL_M1, expressionOptimizer.expression(LessExpression(SL_ONE, SL_TWO), symbolTable))
    }

    @Test
    fun shouldNotReplaceLTIntegerAndStringLiterals() {
        val expression = LessExpression(IL_1, SL_ONE)
        assertEquals(expression, expressionOptimizer.expression(expression, symbolTable))
    }

    @Test
    fun shouldNotReplaceNEIntegerAndStringLiterals() {
        val expression = NotEqualExpression(IL_1, SL_ONE)
        assertEquals(expression, expressionOptimizer.expression(expression, symbolTable))
    }

    @Test
    fun shouldReplaceGEIntegerLiteralsWithOneLiteral() {
        assertEquals(IL_M1, expressionOptimizer.expression(GreaterOrEqualExpression(IL_3, IL_1), symbolTable))
    }

    @Test
    fun shouldReplaceLEIntegerLiteralsWithOneLiteral() {
        assertEquals(IL_0, expressionOptimizer.expression(LessOrEqualExpression(IL_3, IL_1), symbolTable))
    }

    companion object {
        // A minimal type manager that uses the type computation inherited from AbstractTypeManager,
        // so that the tests can verify folds that depend on integer and float operand types
        private val typeManager = object : AbstractTypeManager() {
            override fun getTypeName(type: Type): String = type.javaClass.simpleName
            override fun isKnownAssignableFrom(thisType: Type, thatType: Type): Boolean = thisType == thatType
        }
        private val expressionOptimizer = DefaultAstExpressionOptimizer(typeManager)

        // An optimizer whose type manager types comparisons as Bool, as COL's does
        private val boolOptimizer = DefaultAstExpressionOptimizer(object : AbstractTypeManager() {
            override fun getTypeName(type: Type): String = type.javaClass.simpleName
            override fun isKnownAssignableFrom(thisType: Type, thatType: Type): Boolean = thisType == thatType
            override fun getType(expression: Expression): Type =
                if (expression is RelationalExpression) Bool.INSTANCE else super.getType(expression)
        })
    }
}
