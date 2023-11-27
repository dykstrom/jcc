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
import org.junit.Assert.assertThrows
import org.junit.Test
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.compiler.DefaultTypeManager
import se.dykstrom.jcc.common.error.InvalidValueException
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.types.Str

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
        val addExpression = AddExpression(0, 0, IL_2, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression, symbolTable)

        // Then
        assertEquals(IL_3, optimizedExpression)
    }

    @Test
    fun shouldReplaceAddZeroAndLiteralWithOneLiteral() {
        // Given
        val addExpression = AddExpression(0, 0, IL_0, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression, symbolTable)

        // Then
        assertEquals(IL_1, optimizedExpression)
    }

    @Test
    fun shouldReplaceAddZeroAndIdeWithOneIde() {
        // Given
        val addExpression = AddExpression(0, 0, IDE_I64_A, IL_0)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression, symbolTable)

        // Then
        assertEquals(IDE_I64_A, optimizedExpression)
    }

    @Test
    fun shouldReplaceAddIntegerLiteralsInTreeWithOneLiteral() {
        // Given
        val addExpression1 = AddExpression(0, 0, IL_2, IL_1)
        val addExpression2 = AddExpression(0, 0, IDE_I64_A, addExpression1)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression2, symbolTable)

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
        val optimizedExpressions = expressionOptimizer.expressions(
            listOf(addExpression1, addExpression2, addExpression3),
            SymbolTable()
        )

        // Then
        val expectedExpressions = listOf(IL_3, IL_6, IL_0)
        assertEquals(expectedExpressions, optimizedExpressions)
    }

    @Test
    fun shouldReplaceAddStringLiteralsWithOneLiteral() {
        // Given
        val addExpression = AddExpression(0, 0, SL_ONE, SL_TWO)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression, symbolTable)

        // Then
        assertEquals(SL_ONE_TWO, optimizedExpression)
    }

    @Test
    fun shouldReplaceAddFloatLiteralsWithOneLiteral() {
        // Given
        val addExpression = AddExpression(0, 0, FL_1_00, FL_3_14)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression, symbolTable)

        // Then
        assertEquals(FloatLiteral::class.java, optimizedExpression.javaClass)
        assertEquals(FL_4_14.asDouble(), (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldReplaceAddFloatAndIntegerWithOneLiteral() {
        // Given
        val addExpression = AddExpression(0, 0, IL_1, FL_3_14)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression, symbolTable)

        // Then
        assertEquals(FloatLiteral::class.java, optimizedExpression.javaClass)
        assertEquals(FL_4_14.asDouble(), (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldNotReplaceAddLiteralAndIdent() {
        // Given
        val addExpression = AddExpression(0, 0, IL_1, IDE_I64_A)

        // When
        val optimizedExpression = expressionOptimizer.expression(addExpression, symbolTable)

        // Then
        assertEquals(addExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceSubIntegerLiteralsWithOneLiteral() {
        // Given
        val subExpression = SubExpression(0, 0, IL_3, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(subExpression, symbolTable)

        // Then
        assertEquals(IL_2, optimizedExpression)
    }

    @Test
    fun shouldReplaceSubZeroWithOneLiteral() {
        // Given
        val subExpression = SubExpression(0, 0, IL_0, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(subExpression, symbolTable)

        // Then
        assertEquals(IL_M1, optimizedExpression)
    }

    @Test
    fun shouldReplaceSubIntegerLiteralsInTreeWithOneLiteral() {
        // Given
        val subExpression1 = SubExpression(0, 0, IL_2, IL_1)
        val subExpression2 = SubExpression(0, 0, IDE_I64_A, subExpression1)

        // When
        val optimizedExpression = expressionOptimizer.expression(subExpression2, symbolTable)

        // Then
        val expectedExpression = SubExpression(0, 0, IDE_I64_A, IL_1)
        assertEquals(expectedExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceSubFloatAndIntegerWithOneLiteral() {
        // Given
        val subExpression = SubExpression(0, 0, IL_1, FL_3_14)

        // When
        val optimizedExpression = expressionOptimizer.expression(subExpression, symbolTable)

        // Then
        assertEquals(FloatLiteral::class.java, optimizedExpression.javaClass)
        assertEquals(-2.14, (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldNotReplaceSubLiteralAndFunctionCall() {
        // Given
        val subExpression = SubExpression(0, 0, IL_1, FCE_FOO)

        // When
        val optimizedExpression = expressionOptimizer.expression(subExpression, symbolTable)

        // Then
        assertEquals(subExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceMulZeroWithZero() {
        // Given
        val mulExpression = MulExpression(0, 0, IL_0, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(IL_0, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceMulZeroAndFunctionCall() {
        // Given
        val mulExpression = MulExpression(0, 0, IL_0, FCE_FOO)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(mulExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceMulOneWithOneLiteral() {
        // Given
        val mulExpression = MulExpression(0, 0, IL_1, IDE_I64_A)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(IDE_I64_A, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceMulLiteralAndIdent() {
        // Given
        val mulExpression = MulExpression(0, 0, IDE_I64_A, IL_3)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(mulExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceMulIntegerLiteralsWithOneLiteral() {
        // Given
        val mulExpression = MulExpression(0, 0, IL_2, IL_3)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(IL_6, optimizedExpression)
    }

    @Test
    fun shouldReplaceMulFloatLiteralsWithOneLiteral() {
        // Given
        val mulExpression = MulExpression(0, 0, FL_2_25, FL_3_14)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(7.065, (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldReplaceMulFloatAndIntegerWithOneLiteral() {
        // Given
        val mulExpression = MulExpression(0, 0, FL_2_25, IL_2)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(4.50, (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldReplaceMulPowerOfTwoWithShift() {
        // Given
        val addExpression = AddExpression(0, 0, IL_2, IL_2)
        val mulExpression = MulExpression(0, 0, IDE_I64_A, addExpression)

        // When
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

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
        val optimizedExpression = expressionOptimizer.expression(mulExpression, symbolTable)

        // Then
        assertEquals(mulExpression, optimizedExpression)
    }

    @Test
    fun shouldReplaceDivZeroByLiteralWithZero() {
        // Given
        val divExpression = DivExpression(0, 0, IL_0, FL_1_00)

        // When
        val optimizedExpression = expressionOptimizer.expression(divExpression, symbolTable)

        // Then
        assertEquals(FL_0_00.asDouble(), (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldReplaceDivZeroByIdeWithZero() {
        // Given
        val divExpression = DivExpression(0, 0, IL_0, IDE_I64_A)

        // When
        val optimizedExpression = expressionOptimizer.expression(divExpression, symbolTable)

        // Then
        assertEquals(FL_0_00.asDouble(), (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldReplaceDivByOneWithOneLiteral() {
        // Given
        val divExpression = DivExpression(0, 0, FL_3_14, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(divExpression, symbolTable)

        // Then
        assertEquals(FL_3_14, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceDivZeroByFunctionCall() {
        // Given
        val divExpression = DivExpression(0, 0, IL_0, FCE_FOO)

        // When
        val optimizedExpression = expressionOptimizer.expression(divExpression, symbolTable)

        // Then
        assertEquals(divExpression, optimizedExpression)
    }

    @Test
    fun shouldFailOnDivByZero() {
        // Given
        val divExpression = DivExpression(0, 0, IL_1, IL_0)

        // When & Then
        assertThrows(InvalidValueException::class.java) { expressionOptimizer.expression(divExpression, symbolTable) }
    }

    @Test
    fun shouldReplaceDivFloatAndIntegerWithOneLiteral() {
        // Given
        val divExpression = DivExpression(0, 0, FL_1_00, IL_2)

        // When
        val optimizedExpression = expressionOptimizer.expression(divExpression, symbolTable)

        // Then
        assertEquals(0.5, (optimizedExpression as FloatLiteral).asDouble(), 0.001)
    }

    @Test
    fun shouldReplaceIDivZeroByLiteralWithZero() {
        // Given
        val iDivExpression = IDivExpression(0, 0, IL_0, IL_3)

        // When
        val optimizedExpression = expressionOptimizer.expression(iDivExpression, symbolTable)

        // Then
        assertEquals(IL_0, optimizedExpression)
    }

    @Test
    fun shouldReplaceIDivZeroByIdeWithZero() {
        // Given
        val iDivExpression = IDivExpression(0, 0, IL_0, IDE_I64_A)

        // When
        val optimizedExpression = expressionOptimizer.expression(iDivExpression, symbolTable)

        // Then
        assertEquals(IL_0, optimizedExpression)
    }

    @Test
    fun shouldReplaceIDivByOneWithLiteral() {
        // Given
        val iDivExpression = IDivExpression(0, 0, IL_3, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(iDivExpression, symbolTable)

        // Then
        assertEquals(IL_3, optimizedExpression)
    }

    @Test
    fun shouldReplaceIDivByOneWithIde() {
        // Given
        val iDivExpression = IDivExpression(0, 0, IDE_I64_A, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(iDivExpression, symbolTable)

        // Then
        assertEquals(IDE_I64_A, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceIDivZeroByFunctionCall() {
        // Given
        val iDivExpression = IDivExpression(0, 0, IL_0, FCE_FOO)

        // When
        val optimizedExpression = expressionOptimizer.expression(iDivExpression, symbolTable)

        // Then
        assertEquals(iDivExpression, optimizedExpression)
    }

    @Test
    fun shouldFailOnIDivByZero() {
        // Given
        val iDivExpression = IDivExpression(0, 0, IL_1, IL_0)

        // When & Then
        assertThrows(InvalidValueException::class.java) { expressionOptimizer.expression(iDivExpression, symbolTable) }
    }

    @Test
    fun shouldReplaceIDivIntegerLiteralsWithOneLiteral() {
        // Given
        val iDivExpression = IDivExpression(0, 0, IL_6, IL_3)

        // When
        val optimizedExpression = expressionOptimizer.expression(iDivExpression, symbolTable)

        // Then
        assertEquals(IL_2, optimizedExpression)
    }

    @Test
    fun shouldReplaceModIntegerLiteralsWithOneLiteral() {
        // Given
        val modExpression = ModExpression(0, 0, IL_3, IL_2)

        // When
        val optimizedExpression = expressionOptimizer.expression(modExpression, symbolTable)

        // Then
        assertEquals(IL_1, optimizedExpression)
    }

    @Test
    fun shouldReplaceModByOneWithZero() {
        // Given
        val modExpression = ModExpression(0, 0, IL_3, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(modExpression, symbolTable)

        // Then
        assertEquals(IL_0, optimizedExpression)
    }

    @Test
    fun shouldReplaceModZeroByLiteralWithZero() {
        // Given
        val modExpression = ModExpression(0, 0, IL_0, IL_3)

        // When
        val optimizedExpression = expressionOptimizer.expression(modExpression, symbolTable)

        // Then
        assertEquals(IL_0, optimizedExpression)
    }

    @Test
    fun shouldFailOnModByZero() {
        // Given
        val modExpression = ModExpression(0, 0, IL_3, IL_0)

        // When & Then
        assertThrows(InvalidValueException::class.java) { expressionOptimizer.expression(modExpression, symbolTable) }
    }

    @Test
    fun shouldReplaceNegateIntegerLiteralWithOneLiteral() {
        // Given
        val negateExpression = NegateExpression(0, 0, IL_M1)

        // When
        val optimizedExpression = expressionOptimizer.expression(negateExpression, symbolTable)

        // Then
        assertEquals(IL_1, optimizedExpression)
    }

    @Test
    fun shouldReplaceNegateFloatLiteralWithOneLiteral() {
        // Given
        val negateExpression = NegateExpression(0, 0, FL_3_14)

        // When
        val optimizedExpression = expressionOptimizer.expression(negateExpression, symbolTable)

        // Then
        assertEquals(FL_M3_14, optimizedExpression)
    }

    @Test
    fun shouldReplaceNegateSubExpressionWithOneLiteral() {
        // Given
        val negateExpression = NegateExpression(0, 0, SubExpression(0, 0, IL_3, IL_2))

        // When
        val optimizedExpression = expressionOptimizer.expression(negateExpression, symbolTable)

        // Then
        assertEquals(IL_M1, optimizedExpression)
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
        val addExpression = AddExpression(0, 0, SL_ONE, IDE_STR_S)
        val optimizedExpression = expressionOptimizer.expression(addExpression, symbolTable)

        // Then
        assertEquals(SL_ONE_TWO, optimizedExpression)
    }

    @Test
    fun shouldReplaceAndWithZeroWithZero() {
        // Given
        val expression = AndExpression(0, 0, IDE_I64_A, IL_0)

        // When
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable)

        // Then
        assertEquals(IL_0, optimizedExpression)
    }

    @Test
    fun shouldNotReplaceAndZeroWithFunctionCall() {
        // Given
        val expression = AndExpression(0, 0, FCE_FOO, IL_0)

        // When
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable)

        // Then
        assertEquals(expression, optimizedExpression)
    }

    @Test
    fun shouldReplaceAndIntegerLiteralsWithOneLiteral() {
        // Given
        val expression = AndExpression(0, 0, IL_2, IL_3)

        // When
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable)

        // Then
        assertEquals(IL_2, optimizedExpression)
    }

    @Test
    fun shouldReplaceOrExprWithZeroWithExpr() {
        // Given
        val expression = OrExpression(0, 0, IDE_I64_A, IL_0)

        // When
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable)

        // Then
        assertEquals(IDE_I64_A, optimizedExpression)
    }

    @Test
    fun shouldReplaceOrIntegerLiteralsWithOneLiteral() {
        // Given
        val expression = OrExpression(0, 0, IL_2, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable)

        // Then
        assertEquals(IL_3, optimizedExpression)
    }

    @Test
    fun shouldReplaceXorExprWithZeroWithExpr() {
        // Given
        val expression = XorExpression(0, 0, IL_0, IL_2)

        // When
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable)

        // Then
        assertEquals(IL_2, optimizedExpression)
    }

    @Test
    fun shouldReplaceXorIntegerLiteralsWithOneLiteral() {
        // Given
        val expression = XorExpression(0, 0, IL_3, IL_1)

        // When
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable)

        // Then
        assertEquals(IL_2, optimizedExpression)
    }

    @Test
    fun shouldReplaceNotIntegerLiteralWithOneLiteral() {
        // Given
        val expression = NotExpression(0, 0, IL_M1)

        // When
        val optimizedExpression = expressionOptimizer.expression(expression, symbolTable)

        // Then
        assertEquals(IL_0, optimizedExpression)
    }

    @Test
    fun shouldReplaceEQIntegerLiteralsWithOneLiteral() {
        assertEquals(IL_0, expressionOptimizer.expression(EqualExpression(0, 0, IL_3, IL_1), symbolTable))
        assertEquals(IL_M1, expressionOptimizer.expression(EqualExpression(0, 0, IL_1, IL_1), symbolTable))
        assertEquals(IL_0, expressionOptimizer.expression(EqualExpression(0, 0, IL_1, IL_3), symbolTable))
    }

    @Test
    fun shouldReplaceEQNumericLiteralsWithOneLiteral() {
        assertEquals(IL_0, expressionOptimizer.expression(EqualExpression(0, 0, IL_6, FL_1_00), symbolTable))
        assertEquals(IL_M1, expressionOptimizer.expression(EqualExpression(0, 0, IL_1, FL_1_00), symbolTable))
        assertEquals(IL_0, expressionOptimizer.expression(EqualExpression(0, 0, IL_M1, FL_1_00), symbolTable))
    }

    @Test
    fun shouldReplaceNEIntegerLiteralsWithOneLiteral() {
        assertEquals(IL_M1, expressionOptimizer.expression(NotEqualExpression(0, 0, IL_3, IL_1), symbolTable))
    }

    @Test
    fun shouldReplaceNEFloatLiteralsWithOneLiteral() {
        assertEquals(IL_M1, expressionOptimizer.expression(NotEqualExpression(0, 0, FL_4_14, FL_3_14), symbolTable))
        assertEquals(IL_0, expressionOptimizer.expression(NotEqualExpression(0, 0, FL_3_14, FL_3_14), symbolTable))
        assertEquals(IL_M1, expressionOptimizer.expression(NotEqualExpression(0, 0, FL_3_14, FL_4_14), symbolTable))
    }

    @Test
    fun shouldReplaceGTIntegerLiteralsWithOneLiteral() {
        assertEquals(IL_M1, expressionOptimizer.expression(GreaterExpression(0, 0, IL_3, IL_1), symbolTable))
    }

    @Test
    fun shouldReplaceLTIntegerLiteralsWithOneLiteral() {
        assertEquals(IL_0, expressionOptimizer.expression(LessExpression(0, 0, IL_3, IL_1), symbolTable))
        assertEquals(IL_0, expressionOptimizer.expression(LessExpression(0, 0, IL_1, IL_1), symbolTable))
        assertEquals(IL_M1, expressionOptimizer.expression(LessExpression(0, 0, IL_1, IL_3), symbolTable))
    }

    @Test
    fun shouldReplaceLTFloatLiteralsWithOneLiteral() {
        assertEquals(IL_0, expressionOptimizer.expression(LessExpression(0, 0, FL_4_14, FL_3_14), symbolTable))
        assertEquals(IL_0, expressionOptimizer.expression(LessExpression(0, 0, FL_4_14, FL_4_14), symbolTable))
        assertEquals(IL_M1, expressionOptimizer.expression(LessExpression(0, 0, FL_3_14, FL_4_14), symbolTable))
    }

    @Test
    fun shouldReplaceLTNumericLiteralsWithOneLiteral() {
        assertEquals(IL_0, expressionOptimizer.expression(LessExpression(0, 0, IL_6, FL_1_00), symbolTable))
        assertEquals(IL_0, expressionOptimizer.expression(LessExpression(0, 0, IL_1, FL_1_00), symbolTable))
        assertEquals(IL_M1, expressionOptimizer.expression(LessExpression(0, 0, IL_M1, FL_1_00), symbolTable))
    }

    @Test
    fun shouldReplaceLTStringLiteralsWithOneLiteral() {
        assertEquals(IL_0, expressionOptimizer.expression(LessExpression(0, 0, SL_TWO, SL_ONE), symbolTable))
        assertEquals(IL_0, expressionOptimizer.expression(LessExpression(0, 0, SL_ONE, SL_ONE), symbolTable))
        assertEquals(IL_M1, expressionOptimizer.expression(LessExpression(0, 0, SL_ONE, SL_TWO), symbolTable))
    }

    @Test
    fun shouldNotReplaceLTIntegerAndStringLiterals() {
        val expression = LessExpression(0, 0, IL_1, SL_ONE)
        assertEquals(expression, expressionOptimizer.expression(expression, symbolTable))
    }

    @Test
    fun shouldNotReplaceNEIntegerAndStringLiterals() {
        val expression = NotEqualExpression(0, 0, IL_1, SL_ONE)
        assertEquals(expression, expressionOptimizer.expression(expression, symbolTable))
    }

    @Test
    fun shouldReplaceGEIntegerLiteralsWithOneLiteral() {
        assertEquals(IL_M1, expressionOptimizer.expression(GreaterOrEqualExpression(0, 0, IL_3, IL_1), symbolTable))
    }

    @Test
    fun shouldReplaceLEIntegerLiteralsWithOneLiteral() {
        assertEquals(IL_0, expressionOptimizer.expression(LessOrEqualExpression(0, 0, IL_3, IL_1), symbolTable))
    }

    companion object {
        private val FL_0_00 = FloatLiteral(0, 0, "0.00")
        private val FL_1_00 = FloatLiteral(0, 0, "1.00")
        private val FL_2_25 = FloatLiteral(0, 0, "2.25")
        private val FL_3_14 = FloatLiteral(0, 0, "3.14")
        private val FL_4_14 = FloatLiteral(0, 0, "4.14")
        private val FL_M3_14 = FloatLiteral(0, 0, "-3.14")

        private val IL_0 = IntegerLiteral(0, 0, "0")
        private val IL_1 = IntegerLiteral(0, 0, "1")
        private val IL_2 = IntegerLiteral(0, 0, "2")
        private val IL_3 = IntegerLiteral(0, 0, "3")
        private val IL_6 = IntegerLiteral(0, 0, "6")
        private val IL_M1 = IntegerLiteral(0, 0, "-1")

        private val SL_ONE = StringLiteral(0, 0, "One")
        private val SL_TWO = StringLiteral(0, 0, "Two")
        private val SL_ONE_TWO = StringLiteral(0, 0, "OneTwo")

        private val IDENT_I64_A = Identifier("a%", I64.INSTANCE)
        private val IDE_I64_A = IdentifierDerefExpression(0, 0, IDENT_I64_A)

        private val IDENT_STR_S = Identifier("s$", Str.INSTANCE)
        private val IDE_STR_S = IdentifierDerefExpression(0, 0, IDENT_STR_S)

        private val FCE_FOO = FunctionCallExpression(0, 0, Identifier("foo", I64.INSTANCE), listOf(IL_1))

        // We have to use the default type manager here, since we don't have access to any other.
        // If this becomes a problem for the tests, we will have to make the default type manager
        // more advanced.
        private val expressionOptimizer = DefaultAstExpressionOptimizer(DefaultTypeManager())
    }
}
