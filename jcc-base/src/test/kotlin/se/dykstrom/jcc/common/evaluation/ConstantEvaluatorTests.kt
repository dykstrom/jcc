/*
 * Copyright (C) 2026 Johan Dykstrom
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

package se.dykstrom.jcc.common.evaluation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.common.BaseTests.Companion.FL_1_0
import se.dykstrom.jcc.common.BaseTests.Companion.IDE_I64_A
import se.dykstrom.jcc.common.BaseTests.Companion.IDENT_I64_A
import se.dykstrom.jcc.common.BaseTests.Companion.IL_0
import se.dykstrom.jcc.common.BaseTests.Companion.IL_1
import se.dykstrom.jcc.common.BaseTests.Companion.IL_2
import se.dykstrom.jcc.common.BaseTests.Companion.IL_3
import se.dykstrom.jcc.common.BaseTests.Companion.IL_5
import se.dykstrom.jcc.common.BaseTests.Companion.IL_M1
import se.dykstrom.jcc.common.BaseTests.Companion.SL_A
import se.dykstrom.jcc.common.BaseTests.Companion.SL_B
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.error.InvalidValueException
import se.dykstrom.jcc.common.error.NonConstantException
import se.dykstrom.jcc.common.evaluation.ConstantEvaluator.CallEvaluator
import se.dykstrom.jcc.common.functions.ExternalFunction
import se.dykstrom.jcc.common.functions.Function
import se.dykstrom.jcc.common.functions.LibraryFunction
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.F32
import se.dykstrom.jcc.common.types.I128
import se.dykstrom.jcc.common.types.I32
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import java.util.Optional

class ConstantEvaluatorTests {

    private val symbols = SymbolTable()

    private val evaluator = ConstantEvaluator(CallEvaluator.NONE, ConstantEvaluator::booleanLiteral)

    // evaluate

    @Test
    fun shouldEvaluateConstantReference() {
        symbols.addConstant(IDENT_I64_A, "3")
        assertEquals(Optional.of(IntegerLiteral(0, 0, "5", I64.INSTANCE)), evaluate(AddExpression(IDE_I64_A, IL_2)))
    }

    @Test
    fun shouldNotEvaluateVariableReference() {
        symbols.addVariable(IDENT_I64_A)
        assertThrows<NonConstantException> { evaluate(AddExpression(IDE_I64_A, IL_2)) }
    }

    @Test
    fun shouldNotEvaluateConstantWithoutValue() {
        // A constant whose initializer failed, which has already been reported
        symbols.addConstant(IDENT_I64_A, null)
        assertEquals(Optional.empty<LiteralExpression>(), evaluate(AddExpression(IDE_I64_A, IL_2)))
    }

    @Test
    fun shouldNotEvaluateDivisionByZero() {
        assertThrows<InvalidValueException> { evaluate(IDivExpression(IL_1, IL_0)) }
    }

    @Test
    fun shouldIgnoreDivisionByZeroInBranchNotTaken() {
        val expression = IfExpression(BooleanLiteral.TRUE, IL_5, IDivExpression(IL_1, IL_0))
        assertEquals(Optional.of(IL_5), evaluate(expression))
    }

    @Test
    fun shouldGiveComparisonResultFromHook() {
        val basicLike = ConstantEvaluator(CallEvaluator.NONE) { node, value ->
            IntegerLiteral(node.line(), node.column(), if (value) "-1" else "0", I64.INSTANCE)
        }
        assertEquals(Optional.of(IL_M1), basicLike.evaluate(LessExpression(IL_1, IL_2), symbols))
        assertEquals(Optional.of(IL_0), basicLike.evaluate(EqualExpression(IL_1, IL_2), symbols))
    }

    @Test
    fun shouldEvaluateConstantCall() {
        val evaluator = ConstantEvaluator(DOUBLE_IT, ConstantEvaluator::booleanLiteral)
        assertEquals(Optional.of(IntegerLiteral(0, 0, "6", I64.INSTANCE)), evaluator.evaluate(call(DOUBLE, IL_3), symbols))
    }

    @Test
    fun shouldNotEvaluateCallThatIsNotConstant() {
        // The call is named, not its non-constant argument
        symbols.addVariable(IDENT_I64_A)
        val call = call(DOUBLE, IDE_I64_A)
        val e = assertThrows<NonConstantException> { evaluate(call) }
        assertEquals("non-constant expression: $call", e.message)
    }

    @Test
    fun shouldNotEvaluateUnresolvedCall() {
        val unresolved = FunctionCallExpression(0, 0, Identifier("foo", FUN_I64_TO_I64), listOf(IL_1))
        assertEquals(Optional.empty<LiteralExpression>(), evaluate(unresolved))
    }

    // fold

    @Test
    fun shouldFoldBinaryExpression() {
        assertEquals(Optional.of(IntegerLiteral(0, 0, "3", I64.INSTANCE)), evaluator.fold(AddExpression(IL_1, IL_2)))
    }

    @Test
    fun shouldFoldUnaryExpression() {
        assertEquals(Optional.of(IntegerLiteral(0, 0, "-3", I64.INSTANCE)), evaluator.fold(NegateExpression(IL_3)))
    }

    @Test
    fun shouldFoldWithWrapping() {
        val max = IntegerLiteral(0, 0, "2147483647", I32.INSTANCE)
        val one = IntegerLiteral(0, 0, "1", I32.INSTANCE)
        assertEquals(Optional.of(IntegerLiteral(0, 0, "-2147483648", I32.INSTANCE)), evaluator.fold(AddExpression(max, one)))
    }

    @Test
    fun shouldFoldConstantCall() {
        val evaluator = ConstantEvaluator(DOUBLE_IT, ConstantEvaluator::booleanLiteral)
        assertEquals(Optional.of(IntegerLiteral(0, 0, "6", I64.INSTANCE)), evaluator.fold(call(DOUBLE, IL_3)))
    }

    @Test
    fun shouldNotFoldOperandThatIsNotLiteral() {
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(AddExpression(IDE_I64_A, IL_2)))
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(AddExpression(AddExpression(IL_1, IL_2), IL_2)))
    }

    @Test
    fun shouldNotFoldConstantReference() {
        // A reference is never looked up. The optimizer replaces it in its own scope-aware fold
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(IDE_I64_A))
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(NegateExpression(IDE_I64_A)))
    }

    @Test
    fun shouldNotFoldIfExpression() {
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(IfExpression(BooleanLiteral.TRUE, IL_1, IL_2)))
    }

    @Test
    fun shouldNotFoldDivisionByZero() {
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(IDivExpression(IL_1, IL_0)))
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(ModExpression(IL_1, IL_0)))
    }

    @Test
    fun shouldNotFoldOverflowingDivision() {
        val min = IntegerLiteral(0, 0, Long.MIN_VALUE.toString(), I64.INSTANCE)
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(IDivExpression(min, IL_M1)))
    }

    @Test
    fun shouldNotFoldCastOutOfRange() {
        val big = FloatLiteral(0, 0, "1.0E10", F64.INSTANCE)
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(CastToIntExpression(big, I32.INSTANCE)))
        assertEquals(Optional.of(IntegerLiteral(0, 0, "1", I32.INSTANCE)), evaluator.fold(CastToIntExpression(FL_1_0, I32.INSTANCE)))
    }

    @Test
    fun shouldFoldStringComparison() {
        assertEquals(Optional.of(BooleanLiteral.TRUE), evaluator.fold(LessExpression(SL_A, SL_B)))
        assertEquals(Optional.of(BooleanLiteral.FALSE), evaluator.fold(GreaterOrEqualExpression(SL_A, SL_B)))
        assertEquals(Optional.of(BooleanLiteral.TRUE), evaluator.fold(LessOrEqualExpression(SL_A, SL_A)))
    }

    @Test
    fun shouldCompareStringsLikeStrcmp() {
        // strcmp compares UTF-8 bytes, so a character above U+FFFF sorts after U+FFFD,
        // although its first UTF-16 code unit, a surrogate, sorts before it
        val emoji = StringLiteral(0, 0, "\uD83D\uDE00")
        val replacement = StringLiteral(0, 0, "\uFFFD")
        assertEquals(Optional.of(BooleanLiteral.FALSE), evaluator.fold(LessExpression(emoji, replacement)))
        assertEquals(Optional.of(BooleanLiteral.TRUE), evaluator.fold(GreaterExpression(emoji, replacement)))
        assertEquals(Optional.of(BooleanLiteral.TRUE), evaluator.fold(LessExpression(StringLiteral(0, 0, "a"), replacement)))
    }

    @Test
    fun shouldNotFoldOperandOfWrongType() {
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(NegateExpression(SL_A)))
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(DivExpression(IL_1, IL_2)))
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(AndExpression(FL_1_0, FL_1_0)))
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(LessExpression(SL_A, IL_1)))
    }

    @Test
    fun shouldNotFoldCallThatIsNotConstant() {
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(call(DOUBLE, IL_3)))
    }

    // i128

    @Test
    fun shouldFoldI128BeyondI64() {
        val two = i128("2")
        val seven = i128("7")
        assertI128("18446744073709551614", MulExpression(i128(Long.MAX_VALUE.toString()), two))
        assertI128("9223372036854775808", AddExpression(i128(Long.MAX_VALUE.toString()), i128("1")))
        assertI128("-9223372036854775809", SubExpression(i128(Long.MIN_VALUE.toString()), i128("1")))
        assertI128("24305883351495604533098186245126300818", IDivExpression(i128(I128_MAX), seven))
        assertI128("1", ModExpression(i128(I128_MAX), seven))
        assertI128("-2", ModExpression(i128(I128_MIN), seven))
        assertI128("7", AndExpression(i128(I128_MAX), seven))
        assertI128("-1", OrExpression(i128(I128_MIN), i128(I128_MAX)))
        assertI128("-1", XorExpression(i128(I128_MIN), i128(I128_MAX)))
        assertI128("-$I128_MAX", NegateExpression(i128(I128_MAX)))
        assertI128(I128_MIN, NotExpression(i128(I128_MAX)))
    }

    @Test
    fun shouldWrapI128At128Bits() {
        assertI128(I128_MIN, AddExpression(i128(I128_MAX), i128("1")))
        assertI128(I128_MAX, SubExpression(i128(I128_MIN), i128("1")))
        assertI128("-2", MulExpression(i128(I128_MAX), i128("2")))
        assertI128(I128_MIN, NegateExpression(i128(I128_MIN)))
    }

    @Test
    fun shouldNotFoldOverflowingI128Division() {
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(IDivExpression(i128(I128_MIN), i128("-1"))))
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(ModExpression(i128(I128_MIN), i128("-1"))))
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(IDivExpression(i128(I128_MAX), i128("0"))))
        // The smallest i64 is not the smallest i128
        assertI128("9223372036854775808", IDivExpression(i128(Long.MIN_VALUE.toString()), i128("-1")))
    }

    @Test
    fun shouldCompareI128BeyondI64() {
        val big = i128("18446744073709551616")
        val bigger = i128("18446744073709551617")
        assertEquals(Optional.of(BooleanLiteral.TRUE), evaluator.fold(LessExpression(big, bigger)))
        assertEquals(Optional.of(BooleanLiteral.FALSE), evaluator.fold(GreaterOrEqualExpression(big, bigger)))
        assertEquals(Optional.of(BooleanLiteral.FALSE), evaluator.fold(EqualExpression(big, bigger)))
        assertEquals(Optional.of(BooleanLiteral.TRUE), evaluator.fold(NotEqualExpression(big, bigger)))
        assertEquals(Optional.of(BooleanLiteral.TRUE), evaluator.fold(LessExpression(i128(I128_MIN), i128(I128_MAX))))
    }

    @Test
    fun shouldCastBetweenIntegerWidths() {
        // Widening keeps the value, and narrowing keeps the low bits
        assertI128("-5", CastToIntExpression(IntegerLiteral(0, 0, "-5", I32.INSTANCE), I128.INSTANCE))
        assertI128(Long.MIN_VALUE.toString(), CastToIntExpression(IntegerLiteral(0, 0, Long.MIN_VALUE.toString(), I64.INSTANCE), I128.INSTANCE))
        assertEquals(Optional.of(IntegerLiteral(0, 0, "-1", I64.INSTANCE)), evaluator.fold(CastToIntExpression(i128(I128_MAX), I64.INSTANCE)))
        assertEquals(Optional.of(IntegerLiteral(0, 0, "0", I64.INSTANCE)), evaluator.fold(CastToIntExpression(i128("18446744073709551616"), I64.INSTANCE)))
        assertEquals(Optional.of(IntegerLiteral(0, 0, "1", I32.INSTANCE)), evaluator.fold(CastToIntExpression(i128("18446744073709551617"), I32.INSTANCE)))
        assertEquals(Optional.of(IntegerLiteral(0, 0, "-2147483648", I32.INSTANCE)), evaluator.fold(CastToIntExpression(IntegerLiteral(0, 0, "2147483648", I64.INSTANCE), I32.INSTANCE)))
    }

    @Test
    fun shouldCastFloatToI128() {
        assertI128("100000000000000000000", CastToIntExpression(FloatLiteral(0, 0, "1.0E20", F64.INSTANCE), I128.INSTANCE))
        assertI128("-100000000000000000000", CastToIntExpression(FloatLiteral(0, 0, "-1.0E20", F64.INSTANCE), I128.INSTANCE))
        // 2^127 does not fit, but -2^127 does
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(CastToIntExpression(FloatLiteral(0, 0, "1.7014118346046923E38", F64.INSTANCE), I128.INSTANCE)))
        assertI128(I128_MIN, CastToIntExpression(FloatLiteral(0, 0, "-1.7014118346046923E38", F64.INSTANCE), I128.INSTANCE))
    }

    @Test
    fun shouldCastFloatToNarrowIntegerAtLimits() {
        // A float is truncated toward zero before the range check
        assertEquals(Optional.of(IntegerLiteral(0, 0, "-2147483648", I32.INSTANCE)), evaluator.fold(CastToIntExpression(FloatLiteral(0, 0, "-2147483648.9", F64.INSTANCE), I32.INSTANCE)))
        assertEquals(Optional.of(IntegerLiteral(0, 0, "2147483647", I32.INSTANCE)), evaluator.fold(CastToIntExpression(FloatLiteral(0, 0, "2147483647.9", F64.INSTANCE), I32.INSTANCE)))
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(CastToIntExpression(FloatLiteral(0, 0, "2147483648.0", F64.INSTANCE), I32.INSTANCE)))
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(CastToIntExpression(FloatLiteral(0, 0, "-2147483649.0", F64.INSTANCE), I32.INSTANCE)))
        assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(CastToIntExpression(FloatLiteral(0, 0, "9.223372036854775808E18", F64.INSTANCE), I64.INSTANCE)))
        assertEquals(Optional.of(IntegerLiteral(0, 0, Long.MIN_VALUE.toString(), I64.INSTANCE)), evaluator.fold(CastToIntExpression(FloatLiteral(0, 0, "-9.223372036854775808E18", F64.INSTANCE), I64.INSTANCE)))
        listOf("NaN", "Infinity", "-Infinity").forEach {
            assertEquals(Optional.empty<LiteralExpression>(), evaluator.fold(CastToIntExpression(FloatLiteral(0, 0, it, F64.INSTANCE), I128.INSTANCE)))
        }
    }

    @Test
    fun shouldCastI128ToFloat() {
        assertEquals(Optional.of(FloatLiteral(0, 0, "1.8446744073709552E19", F64.INSTANCE)), evaluator.fold(CastToFloatExpression(i128("18446744073709551616"), F64.INSTANCE)))
        assertEquals(Optional.of(FloatLiteral(0, 0, "1.7014118346046923E38", F64.INSTANCE)), evaluator.fold(CastToFloatExpression(i128(I128_MAX), F64.INSTANCE)))
        assertEquals(Optional.of(FloatLiteral(0, 0, "1.8446744E19", F32.INSTANCE)), evaluator.fold(CastToFloatExpression(i128("18446744073709551616"), F32.INSTANCE)))
    }

    private fun i128(value: String) = IntegerLiteral(0, 0, value, I128.INSTANCE)

    private fun assertI128(expected: String, expression: Expression) =
        assertEquals(Optional.of<LiteralExpression>(i128(expected)), evaluator.fold(expression))

    private fun evaluate(expression: Expression) = evaluator.evaluate(expression, symbols)

    private fun call(function: Function, vararg args: Expression) =
        FunctionCallExpression(Identifier(function.name, FUN_I64_TO_I64), args.toList(), function)

    companion object {
        private const val I128_MAX = "170141183460469231731687303715884105727"
        private const val I128_MIN = "-170141183460469231731687303715884105728"

        private val FUN_I64_TO_I64: Fun = Fun.from(listOf(I64.INSTANCE), I64.INSTANCE)

        private val DOUBLE = LibraryFunction("double", listOf(I64.INSTANCE), I64.INSTANCE, ExternalFunction("double"))

        /** Evaluates calls to [DOUBLE], and no other function. */
        private val DOUBLE_IT = object : CallEvaluator {
            override fun isConstant(function: Function) = function == DOUBLE

            override fun evaluate(call: FunctionCallExpression, args: List<LiteralExpression>): LiteralExpression {
                val value = (args.single() as IntegerLiteral).asBigInteger().shiftLeft(1)
                return IntegerLiteral(call.line(), call.column(), value.toString(), I64.INSTANCE)
            }
        }
    }
}
