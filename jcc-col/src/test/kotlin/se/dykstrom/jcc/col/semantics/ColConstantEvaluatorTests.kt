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

package se.dykstrom.jcc.col.semantics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.dykstrom.jcc.col.compiler.ColSemanticsParser
import se.dykstrom.jcc.col.compiler.ColSymbols
import se.dykstrom.jcc.col.compiler.ColSyntaxParser
import se.dykstrom.jcc.col.type.ColTypeManager
import se.dykstrom.jcc.common.ast.BooleanLiteral
import se.dykstrom.jcc.common.ast.ConstDeclarationStatement
import se.dykstrom.jcc.common.ast.LiteralExpression
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.error.SemanticsException
import se.dykstrom.jcc.common.types.F32
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I32
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Str
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets.UTF_8

/**
 * Tests the evaluator through the semantics parser, which types the initializer and makes
 * implicit widening explicit before the evaluator sees it. Each test declares the constants
 * it needs, and returns the value of the last one.
 */
class ColConstantEvaluatorTests {

    private var errorListener = CompilationErrorListener()

    // Integers

    @Test
    fun shouldEvaluateIntegerArithmetic() {
        assertInteger("3", "1 + 2")
        assertInteger("-1", "1 - 2")
        assertInteger("42", "6 * 7")
        assertInteger("3", "7 div 2")
        assertInteger("-3", "-7 div 2")
        assertInteger("-1", "-7 mod 2")
        assertInteger("1", "7 mod 2")
    }

    @Test
    fun shouldEvaluateBitwiseOperators() {
        assertInteger("2", "6 & 3")
        assertInteger("7", "6 | 3")
        assertInteger("5", "6 ^ 3")
        assertInteger("-7", "~6")
    }

    @Test
    fun shouldNegateConstant() {
        assertInteger("-17", "-A", "const A := 17")
    }

    @Test
    fun shouldWrapI64() {
        assertInteger("-9223372036854775808", "9223372036854775807 + 1")
        assertInteger("9223372036854775807", "-A - 1", "const A := 9223372036854775807 + 1")
    }

    @Test
    fun shouldWrapI32() {
        assertValue("-2147483648", I32.INSTANCE, "2147483647i32 + 1i32")
        assertValue("-2", I32.INSTANCE, "2147483647i32 * 2i32")
        assertValue("-2147483648", I32.INSTANCE, "-A", "const A := -2147483648i32")
    }

    @Test
    fun shouldWidenI32ToI64() {
        assertInteger("2147483648", "2147483647i32 + 1")
    }

    // Floats

    @Test
    fun shouldEvaluateFloatArithmetic() {
        assertFloat((0.1 + 0.2).toString(), "0.1 + 0.2")
        assertFloat("0.5", "1.0 / 2.0")
        assertFloat("-1.5", "-A", "const A := 1.5")
    }

    @Test
    fun shouldEvaluateF32InSinglePrecision() {
        assertValue((0.1f + 0.2f).toString(), F32.INSTANCE, "0.1f32 + 0.2f32")
        assertValue((0.1f * 3.0f).toString(), F32.INSTANCE, "0.1f32 * 3.0f32")
    }

    @Test
    fun shouldDivideInOperandPrecision() {
        assertValue((1.0f / 3.0f).toString(), F32.INSTANCE, "1.0f32 / 3.0f32")
        assertFloat((1.0 / 3.0).toString(), "1.0 / 3.0")
        // The f32 operand is widened to f64
        assertFloat((1.0f.toDouble() / 3.0).toString(), "1.0f32 / 3.0")
    }

    @Test
    fun shouldFollowIeee754() {
        assertFloat("Infinity", "1.0 / Z", "const Z := 0.0")
        assertFloat("-Infinity", "-1.0 / Z", "const Z := 0.0")
        assertFloat("NaN", "Z / Z", "const Z := 0.0")
        assertValue("Infinity", F32.INSTANCE, "f32(1e300)")
    }

    // Comparisons and booleans

    @Test
    fun shouldCompareIntegers() {
        assertBoolean(true, "1 < 2")
        assertBoolean(false, "2 <= 1")
        assertBoolean(true, "-1 > -2")
        assertBoolean(true, "2 >= 2")
        assertBoolean(true, "2 == 2")
        assertBoolean(true, "2 != 3")
    }

    @Test
    fun shouldCompareFloats() {
        assertBoolean(true, "1.5 < 2.5")
        assertBoolean(true, "0.0 == -0.0")
        assertBoolean(false, "N == N", "const N := Z / Z", "const Z := 0.0")
        assertBoolean(true, "N != N", "const N := Z / Z", "const Z := 0.0")
        assertBoolean(false, "N < 1.0", "const N := Z / Z", "const Z := 0.0")
    }

    @Test
    fun shouldEvaluateLogicalOperators() {
        assertBoolean(false, "true and false")
        assertBoolean(true, "true or false")
        assertBoolean(false, "true xor true")
        assertBoolean(false, "not true")
        assertBoolean(true, "true == true")
    }

    // Strings

    @Test
    fun shouldEvaluateStrings() {
        assertValue("ab", Str.INSTANCE, "\"a\" + \"b\"")
        assertBoolean(true, "\"a\" == \"a\"")
        assertBoolean(true, "\"a\" != \"b\"")
    }

    // Casts and conversions

    @Test
    fun shouldCastBetweenIntegers() {
        assertValue("1", I32.INSTANCE, "i32(4294967297)")
        assertValue("-1", I64.INSTANCE, "i64(-1i32)")
        assertValue("17", I64.INSTANCE, "i64(17)")
    }

    @Test
    fun shouldCastFloatsToIntegersTowardZero() {
        assertValue("3", I32.INSTANCE, "i32(3.9)")
        assertValue("-3", I64.INSTANCE, "i64(-3.9)")
        assertValue("2", I64.INSTANCE, "i64(2.5f32)")
    }

    @Test
    fun shouldCastIntegersToFloats() {
        assertFloat("1.0", "f64(1i32)")
        // 16777217 is not representable in single precision
        assertValue("1.6777216E7", F32.INSTANCE, "f32(16777217)")
        // Converting directly to single precision rounds once, not twice
        assertValue((9007199791611905L).toFloat().toString(), F32.INSTANCE, "f32(9007199791611905)")
    }

    @Test
    fun shouldCastBetweenFloats() {
        assertValue((0.1f).toString(), F32.INSTANCE, "f32(0.1)")
        assertFloat((0.1f).toDouble().toString(), "f64(0.1f32)")
    }

    @Test
    fun shouldEvaluateOpaqueConversions() {
        assertFloat("9.5", "Meters(7.3) + Meters(2.2)", "type Meters as f64")
        assertFloat("1.5", "f64(M)", "const M := Meters(1.5)", "type Meters as f64")
        assertValue("Bob", Str.INSTANCE, "N + Name(\"b\")", "const N := Name(\"Bo\")", "type Name as string")
    }

    // If-expressions

    @Test
    fun shouldEvaluateIfExpression() {
        assertInteger("10", "if 1 < 2 then 10 else 20")
        assertInteger("20", "if 1 > 2 then 10 else 20")
    }

    @Test
    fun shouldIgnoreErrorInBranchNotTaken() {
        assertInteger("0", "if B == 0 then 0 else 17 div B", "const B := 0")
    }

    // Errors

    @Test
    fun shouldNotDivideByZero() {
        assertError("division by zero", "17 div Z", "const Z := 0")
        assertError("division by zero", "17 mod Z", "const Z := 0")
        assertError("division by zero", "if true then 17 div Z else 0", "const Z := 0")
    }

    @Test
    fun shouldNotOverflowDivision() {
        assertError("integer overflow", "-9223372036854775808 div -1")
        assertError("integer overflow", "-2147483648i32 mod -1i32")
    }

    @Test
    fun shouldNotCastFloatOutOfRange() {
        assertError("does not fit in i32", "i32(2147483648.0)")
        assertError("does not fit in i64", "i64(1e19)")
        assertError("does not fit in i64", "i64(Z / Z)", "const Z := 0.0")
    }

    @Test
    fun shouldCastFloatAtTheEdgeOfTheRange() {
        assertValue("2147483647", I32.INSTANCE, "i32(2147483647.9)")
        assertValue("-2147483648", I32.INSTANCE, "i32(-2147483648.9)")
        assertValue("-9223372036854775808", I64.INSTANCE, "i64(-9.223372036854775808e18)")
    }

    private fun assertInteger(expected: String, initializer: String, vararg before: String) =
        assertValue(expected, I64.INSTANCE, initializer, *before)

    private fun assertFloat(expected: String, initializer: String, vararg before: String) =
        assertValue(expected, F64.INSTANCE, initializer, *before)

    private fun assertBoolean(expected: Boolean, initializer: String, vararg before: String) {
        val value = evaluate(initializer, *before)
        assertEquals(if (expected) BooleanLiteral.TRUE else BooleanLiteral.FALSE, value)
    }

    private fun assertValue(expected: String, expectedType: Any, initializer: String, vararg before: String) {
        val value = evaluate(initializer, *before)
        assertEquals(expected, value.value)
        assertEquals(expectedType, value.type())
    }

    private fun assertError(expected: String, initializer: String, vararg before: String) {
        assertThrows<SemanticsException> { evaluate(initializer, *before) }
        assertTrue(errorListener.errors.any { it.msg.contains(expected) }, errorListener.errors.toString())
    }

    /**
     * Parses a program that declares the constants in [before], in reverse order, followed by
     * a constant initialized with [initializer], and returns the value of that constant.
     */
    private fun evaluate(initializer: String, vararg before: String): LiteralExpression {
        errorListener = CompilationErrorListener()
        val text = (before.reversed() + "const X := $initializer").joinToString("\n")
        val syntaxParser = ColSyntaxParser(errorListener)
        val semanticsParser = ColSemanticsParser(errorListener, ColSymbols(), ColTypeManager())
        val program = semanticsParser.parse(syntaxParser.parse(ByteArrayInputStream(text.toByteArray(UTF_8))))
        assertFalse(errorListener.hasErrors())
        val statement = program.statements.last() as ConstDeclarationStatement
        return statement.declarations.single().expression() as LiteralExpression
    }
}
