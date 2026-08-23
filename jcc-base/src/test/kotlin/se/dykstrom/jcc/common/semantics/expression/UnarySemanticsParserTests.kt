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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.compiler.AbstractSemanticsParser
import se.dykstrom.jcc.common.compiler.AbstractTypeManager
import se.dykstrom.jcc.common.compiler.TypeManager
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.semantics.expression.OperandTypeRule.*
import se.dykstrom.jcc.common.symbols.SymbolTable
import se.dykstrom.jcc.common.types.*

/**
 * Tests the operand rules of [UnarySemanticsParser], independently of any language front end.
 * The rules are the same objects [BinarySemanticsParser] composes, so these tests pin what one
 * operand does to a rule written for any arity, and the wording it produces for a single operand.
 */
class UnarySemanticsParserTests {

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

    @Test
    fun shouldParseOperand() {
        parse(NegateExpression(IL_1), "negate", NUMERIC)
        assertEquals(listOf<Expression>(IL_1), visitedOperands)
    }

    @Test
    fun shouldReplaceOperandWithParsedOne() {
        rewriteOperand = { IL_17 }
        assertEquals(NegateExpression(IL_17), parse(NegateExpression(IL_1), "negate", NUMERIC))
        assertNoErrors()
    }

    @Test
    fun shouldAcceptOperandSatisfyingRule() {
        assertEquals(NegateExpression(IL_1), parse(NegateExpression(IL_1), "negate", NUMERIC))
        assertNoErrors()
    }

    @Test
    fun shouldReportNumericRuleWithoutRequirementClause() {
        // NUMERIC is the widest rule, so naming the type says everything - matching the binary
        // "cannot add string and i64", which likewise appends nothing
        parse(NegateExpression(SL_A), "negate", NUMERIC)
        assertError("cannot negate string")
    }

    @Test
    fun shouldReportIntegerRuleWithRequirementClause() {
        parse(NotExpression(FL_1_0), "bitwise-not", INTEGER)
        assertError("cannot bitwise-not f64: the operand must be an integer")
    }

    @Test
    fun shouldReportBooleanRuleWithRequirementClause() {
        parse(LogicalNotExpression(0, 0, IL_1), "logical-not", BOOLEAN)
        assertError("cannot logical-not i64: the operand must be boolean")
    }

    @Test
    fun shouldReportErrorAtExpressionPosition() {
        parse(NegateExpression(3, 7, SL_A), "negate", NUMERIC)
        val error = errorListener.errors.first()
        assertEquals(3, error.line)
        assertEquals(7, error.column)
    }

    @Test
    fun shouldReturnExpressionAfterError() {
        // Analysis continues after an error, on an expression with its operand parsed
        assertEquals(NegateExpression(SL_A), parse(NegateExpression(SL_A), "negate", NUMERIC))
    }

    private fun parse(expression: UnaryExpression,
                      operation: String,
                      rule: OperandTypeRule): Expression =
        UnarySemanticsParser(parser, operation, rule).parse(expression)

    private fun assertNoErrors() =
        assertTrue(errorListener.errors.isEmpty(), "expected no errors, was ${errorListener.errors}")

    private fun assertError(expectedMsg: String) {
        assertEquals(listOf(expectedMsg), errorListener.errors.map { it.msg })
    }

    companion object {
        private val IL_1 = IntegerLiteral(0, 0, 1L)
        private val IL_17 = IntegerLiteral(0, 0, 17L)
        private val FL_1_0 = FloatLiteral(0, 0, 1.0)
        private val SL_A = StringLiteral(0, 0, "a")

        private val TYPE_MANAGER = object : AbstractTypeManager() {
            override fun getTypeName(type: Type): String = when (type) {
                Bool.INSTANCE -> "bool"
                F64.INSTANCE -> "f64"
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
