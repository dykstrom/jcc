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

package se.dykstrom.jcc.common.semantics.expression

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.compiler.AbstractSemanticsParser
import se.dykstrom.jcc.common.compiler.AbstractTypeManager
import se.dykstrom.jcc.common.compiler.TypeManager
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.error.InvalidValueException
import se.dykstrom.jcc.common.semantics.expression.OperandTypeRule.*
import se.dykstrom.jcc.common.semantics.expression.OperandValueRule.NON_ZERO_DIVISOR
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.*

/**
 * Tests the operand rules and the operand promotion of [BinarySemanticsParser], independently of
 * any language front end.
 */
class BinarySemanticsParserTests {

    private val errorListener = CompilationErrorListener()

    /** The operands the component asked the enclosing parser to parse, in order. */
    private val visitedOperands = mutableListOf<Expression>()

    /** What the enclosing parser returns for each operand it visits. The identity by default. */
    private var rewriteOperand: (Expression) -> Expression = { it }

    private val parser = object : AbstractSemanticsParser<TypeManager>(errorListener, SymbolTable(), TYPE_MANAGER) {
        override fun parse(program: AstProgram) = program

        override fun expression(expression: Expression): Expression {
            visitedOperands.add(expression)
            return rewriteOperand(expression)
        }
    }

    // Operands

    @Test
    fun shouldParseBothOperands() {
        parse(AddExpression(IL_1, IL_2), "add", NUMERIC)
        assertEquals(listOf(IL_1, IL_2), visitedOperands)
    }

    @Test
    fun shouldReplaceOperandsWithParsedOnes() {
        rewriteOperand = { if (it == IL_1) IL_17 else it }
        assertEquals(AddExpression(IL_17, IL_2), parse(AddExpression(IL_1, IL_2), "add", NUMERIC))
        assertNoErrors()
    }

    // Operand type rules

    @Test
    fun shouldAcceptOperandsSatisfyingRule() {
        assertEquals(AddExpression(IL_1, IL_2), parse(AddExpression(IL_1, IL_2), "add", NUMERIC))
        assertNoErrors()
    }

    @Test
    fun shouldReportOperandsViolatingRule() {
        parse(AndExpression(FL_1_0, FL_2_0), "bitwise-and", INTEGER)
        assertError("cannot bitwise-and f64 and f64: both operands must be integers")
    }

    @Test
    fun shouldReportOperandsViolatingFloatRule() {
        parse(DivExpression(IL_1, IL_2), "divide", FLOAT)
        assertError("cannot divide i64 and i64: both operands must be floating point")
    }

    @Test
    fun shouldReportOperandsViolatingBooleanRule() {
        parse(LogicalAndExpression(IL_1, IL_2), "logical-and", BOOLEAN)
        assertError("cannot logical-and i64 and i64: both operands must be boolean")
    }

    @Test
    fun shouldAcceptAnyEqualTypesWithoutTypeRules() {
        assertEquals(EqualExpression(SL_A, SL_B), parse(EqualExpression(SL_A, SL_B), "compare"))
        assertNoErrors()
    }

    @Test
    fun shouldRequireAllTypeRules() {
        // NUMERIC accepts two floats, INTEGER does not
        parse(AddExpression(FL_1_0, FL_2_0), "add", NUMERIC, INTEGER)
        assertError("cannot add f64 and f64: both operands must be integers")
    }

    @Test
    fun shouldReportFirstViolatedTypeRule() {
        // Both rules reject two strings, but only the first one reports
        parse(GreaterExpression(SL_A, SL_B), "compare", NOT_STRINGS, NUMERIC)
        assertError("cannot order strings")
    }

    @Test
    fun shouldAcceptWhatEitherComposedRuleAccepts() {
        assertEquals(AddExpression(SL_A, SL_B), parse(AddExpression(SL_A, SL_B), "add", NUMERIC.or(STRINGS)))
        assertNoErrors()
    }

    @Test
    fun shouldReportComposedRuleWithMessageOfFirstRule() {
        // Reports NUMERIC's message, not STRINGS'. The promotion below the rule check complains
        // about the same operands in the same words at the same position, and the error listener
        // drops that repeat - the developer is told once.
        parse(AddExpression(SL_A, IL_1), "add", NUMERIC.or(STRINGS))
        assertEquals(listOf("cannot add string and i64"), errorListener.errors.map { it.msg })
    }

    @Test
    fun shouldReportErrorAtExpressionPosition() {
        parse(AddExpression(3, 7, SL_A, IL_1), "add", NUMERIC)
        val error = errorListener.errors.first()
        assertEquals(3, error.line)
        assertEquals(7, error.column)
    }

    @Test
    fun shouldReturnExpressionAfterError() {
        // Analysis continues after an error, on an expression with its operands parsed
        assertEquals(AddExpression(SL_A, IL_1), parse(AddExpression(SL_A, IL_1), "add", NUMERIC))
    }

    // Operand promotion

    @Test
    fun shouldNotPromoteEqualTypes() {
        val result = parse(AddExpression(IL_1, IL_2), "add", NUMERIC) as AddExpression
        assertSame(IL_1, result.left)
        assertSame(IL_2, result.right)
        assertNoErrors()
    }

    @Test
    fun shouldPromoteLeftIntegerOperand() {
        val result = parse(AddExpression(IL_1_I32, IL_2), "add", NUMERIC) as AddExpression
        assertCast(I64.INSTANCE, IL_1_I32, result.left)
        assertSame(IL_2, result.right)
        assertNoErrors()
    }

    @Test
    fun shouldPromoteRightIntegerOperand() {
        val result = parse(AddExpression(IL_1, IL_2_I32), "add", NUMERIC) as AddExpression
        assertSame(IL_1, result.left)
        assertCast(I64.INSTANCE, IL_2_I32, result.right)
        assertNoErrors()
    }

    @Test
    fun shouldPromoteLeftFloatOperand() {
        val result = parse(AddExpression(FL_1_0_F32, FL_2_0), "add", NUMERIC) as AddExpression
        assertCast(F64.INSTANCE, FL_1_0_F32, result.left)
        assertSame(FL_2_0, result.right)
        assertNoErrors()
    }

    @Test
    fun shouldPromoteRightFloatOperand() {
        val result = parse(AddExpression(FL_1_0, FL_2_0_F32), "add", NUMERIC) as AddExpression
        assertSame(FL_1_0, result.left)
        assertCast(F64.INSTANCE, FL_2_0_F32, result.right)
        assertNoErrors()
    }

    @Test
    fun shouldReportMixedIntegerAndFloatOperands() {
        // NUMERIC accepts an integer and a float, but only same-family operands can be promoted
        parse(AddExpression(IL_1_I32, FL_2_0), "add", NUMERIC)
        assertError("cannot add i32 and f64")
    }

    @Test
    fun shouldReportUnrelatedTypesWithoutTypeRules() {
        parse(EqualExpression(SL_A, IL_1), "compare")
        assertError("cannot compare string and i64")
    }

    // Operand value rules

    @Test
    fun shouldReportZeroIntegerDivisor() {
        parse(IDivExpression(IL_1, IL_0), "divide", NON_ZERO_DIVISOR, INTEGER)
        assertError("division by zero: 0")
        assertTrue(errorListener.errors.first().exception is InvalidValueException)
    }

    @Test
    fun shouldReportZeroFloatDivisor() {
        parse(DivExpression(FL_1_0, FL_0_0), "divide", NON_ZERO_DIVISOR, FLOAT)
        assertError("division by zero: 0.0")
    }

    @Test
    fun shouldAcceptZeroOperandWithoutValueRule() {
        parse(MulExpression(IL_1, IL_0), "multiply", NUMERIC)
        assertNoErrors()
    }

    private fun parse(expression: BinaryExpression,
                      operation: String,
                      vararg typeRules: OperandTypeRule): Expression =
        BinarySemanticsParser(parser, operation, *typeRules).parse(expression)

    private fun parse(expression: BinaryExpression,
                      operation: String,
                      valueRule: OperandValueRule,
                      vararg typeRules: OperandTypeRule): Expression =
        BinarySemanticsParser(parser, operation, valueRule, *typeRules).parse(expression)

    private fun assertCast(expectedType: Type, expectedOperand: Expression, actual: Expression) {
        val cast = actual as TypedExpression
        assertEquals(expectedType, cast.type())
        assertSame(expectedOperand, (cast as UnaryExpression).expression)
    }

    private fun assertNoErrors() =
        assertTrue(errorListener.errors.isEmpty(), "expected no errors, was ${errorListener.errors}")

    private fun assertError(expectedMsg: String) {
        assertEquals(listOf(expectedMsg), errorListener.errors.map { it.msg })
    }

    companion object {
        private val IL_0 = IntegerLiteral(0, 0, 0L)
        private val IL_1 = IntegerLiteral(0, 0, 1L)
        private val IL_2 = IntegerLiteral(0, 0, 2L)
        private val IL_17 = IntegerLiteral(0, 0, 17L)
        private val IL_1_I32 = IntegerLiteral(0, 0, 1L, I32.INSTANCE)
        private val IL_2_I32 = IntegerLiteral(0, 0, 2L, I32.INSTANCE)

        private val FL_0_0 = FloatLiteral(0, 0, "0.0")
        private val FL_1_0 = FloatLiteral(0, 0, "1.0")
        private val FL_2_0 = FloatLiteral(0, 0, "2.0")
        private val FL_1_0_F32 = FloatLiteral(0, 0, "1.0", F32.INSTANCE)
        private val FL_2_0_F32 = FloatLiteral(0, 0, "2.0", F32.INSTANCE)

        private val SL_A = StringLiteral(0, 0, "a")
        private val SL_B = StringLiteral(0, 0, "b")

        /** A rule of a language's own, to verify that rule order decides which error is reported. */
        private val NOT_STRINGS = ofEachOperand(
            { type -> type !is Str },
            { "cannot order strings" }
        )

        // A minimal type manager that uses the type computation inherited from AbstractTypeManager,
        // and types relational and logical expressions as booleans, the way a language with those
        // operators does
        private val TYPE_MANAGER = object : AbstractTypeManager() {
            override fun getTypeName(type: Type): String = when (type) {
                Bool.INSTANCE -> "bool"
                F32.INSTANCE -> "f32"
                F64.INSTANCE -> "f64"
                I32.INSTANCE -> "i32"
                I64.INSTANCE -> "i64"
                Str.INSTANCE -> "string"
                else -> type.javaClass.simpleName
            }

            override fun isAssignableFrom(thisType: Type, thatType: Type): Boolean = thisType == thatType

            override fun getType(expression: Expression): Type = when (expression) {
                is RelationalExpression, is LogicalExpression -> Bool.INSTANCE
                else -> super.getType(expression)
            }
        }
    }
}
