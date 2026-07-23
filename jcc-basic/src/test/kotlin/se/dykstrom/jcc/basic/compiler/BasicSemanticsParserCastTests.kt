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

package se.dykstrom.jcc.basic.compiler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_1_0
import se.dykstrom.jcc.basic.BasicTests.Companion.FL_3_14
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_F64_F
import se.dykstrom.jcc.basic.BasicTests.Companion.IDE_I64_A
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_F64_F
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_I64_A
import se.dykstrom.jcc.basic.ast.statement.SleepStatement
import se.dykstrom.jcc.basic.ast.statement.SwapStatement
import se.dykstrom.jcc.basic.compiler.BasicSymbols.BF_CDBL_F64
import se.dykstrom.jcc.basic.compiler.BasicSymbols.BF_CINT_F64
import se.dykstrom.jcc.basic.compiler.BasicSymbols.BF_FIX_F64
import se.dykstrom.jcc.basic.compiler.BasicSymbols.BF_INT_F64
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.types.F64
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.llvm.code.LlvmBuiltIns.LF_ROUNDEVEN_F64

/**
 * Verifies that [BasicSemanticsParser] makes implicit numeric casts explicit in the AST at every
 * site where a conversion is accepted (issue #52). Integer→float becomes a [CastToF64Expression];
 * float→integer becomes a truncating [CastToI64Expression] composed with a [RoundExpression] that
 * rounds half-to-even (`llvm.roundeven`, QuickBASIC 4.5 semantics). Code generation then only has
 * to lower the cast it sees. Mirrors COL's `ColSemanticsParserCastTests`.
 *
 * SWAP is the exception: its operands are lvalues, so the cross-type conversion stays a
 * code-generation concern and no expression-level cast appears in the AST.
 */
class BasicSemanticsParserCastTests : AbstractBasicSemanticsParserTests() {

    @BeforeEach
    fun setUp() {
        defineFunction(FUN_SUM1)            // sum(integer) : integer
        defineFunction(BF_CDBL_F64)         // cdbl(double) : double
        defineFunction(BF_CINT_F64)         // cint(double) : integer
        defineFunction(BF_FIX_F64)          // fix(double)  : integer
        defineFunction(BF_INT_F64)          // int(double)  : integer
    }

    // ------------------------------------------------------------------------
    // Implicit cast sites (issue #52 inventory): casts are now inserted.
    // ------------------------------------------------------------------------

    @Test
    fun assignmentIntToFloatInsertsCast() {
        // LET f# = a%  : integer -> double
        val rhs = rhsOf("DIM a% AS INTEGER : DIM f# AS DOUBLE : LET f# = a%")
        assertEquals(castToFloat(IDE_I64_A), rhs)
    }

    @Test
    fun assignmentFloatToIntInsertsRoundingCast() {
        // LET a% = f#  : double -> integer, rounded half-to-even
        val rhs = rhsOf("DIM a% AS INTEGER : DIM f# AS DOUBLE : LET a% = f#")
        assertEquals(castToInt(IDE_F64_F), rhs)
    }

    @Test
    fun binaryIntPlusFloatPromotesIntegerOperand() {
        // a% + 3.14 : the integer operand is promoted to double
        val rhs = rhsOf("DIM a% AS INTEGER : DIM f# AS DOUBLE : LET f# = a% + 3.14") as AddExpression
        assertEquals(castToFloat(IDE_I64_A), rhs.left)
        assertEquals(FL_3_14, rhs.right)
    }

    @Test
    fun relationalIntVsFloatPromotesIntegerOperand() {
        // a% > 3.14 : the integer operand is promoted to double for the comparison
        val rhs = rhsOf("DIM a% AS INTEGER : DIM h% AS INTEGER : LET h% = (a% > 3.14)") as GreaterExpression
        assertEquals(castToFloat(IDE_I64_A), rhs.left)
        assertEquals(FL_3_14, rhs.right)
    }

    @Test
    fun functionArgFloatToIntInsertsRoundingCast() {
        // sum(f#) : double argument to an integer parameter
        val rhs = rhsOf("DIM f# AS DOUBLE : DIM h% AS INTEGER : LET h% = sum(f#)") as FunctionCallExpression
        assertEquals(1, rhs.args.size)
        assertEquals(castToInt(IDE_F64_F), rhs.args[0])
    }

    @Test
    fun functionReturnFloatToIntInsertsRoundingCast() {
        // DEF FNfoo%() = 1.0 : double body for an integer return
        val program = parse("DEF FNfoo%() = 1.0")
        val fds = program.statements[0] as FunctionDefinitionStatement
        assertEquals(castToInt(FL_1_0), fds.expression())
    }

    @Test
    fun arraySubscriptFloatToIntInsertsRoundingCast() {
        // foo(3.14) : float subscript into an integer array
        val rhs = rhsOf("DIM foo(10) AS INTEGER : DIM h% AS INTEGER : LET h% = foo(3.14)") as ArrayAccessExpression
        assertEquals(1, rhs.subscripts.size)
        assertEquals(castToInt(FL_3_14), rhs.subscripts[0])
    }

    @Test
    fun sleepIntArgumentInsertsCast() {
        // SLEEP a% : SLEEP takes a double, so the integer argument is coerced to double
        val program = parse("DIM a% AS INTEGER : SLEEP a%")
        val sleep = program.statements.filterIsInstance<SleepStatement>().single()
        assertEquals(castToFloat(IDE_I64_A), sleep.expression)
    }

    @Test
    fun swapMixedNumericHasNoCast() {
        // SWAP a%, f# : the conversion is a code-generation concern; the SWAP operands are lvalues,
        // so semantic analysis leaves them as bare identifier expressions (no expression-level cast).
        val program = parse("DIM a% AS INTEGER : DIM f# AS DOUBLE : SWAP a%, f#")
        val swap = program.statements.filterIsInstance<SwapStatement>().single()
        assertNotCast(swap.first())
        assertNotCast(swap.second())
        assertEquals(INE_I64_A, swap.first())
        assertEquals(INE_F64_F, swap.second())
    }

    // ------------------------------------------------------------------------
    // Explicit cast functions remain ordinary (non-inlined) function calls.
    // Inlining them to cast nodes is out of scope for issue #52.
    // ------------------------------------------------------------------------

    @Test
    fun cintResolvesToIntegerFunctionCall() {
        assertCastFunctionCall("DIM f# AS DOUBLE : DIM h% AS INTEGER : LET h% = cint(f#)", "cint", I64.INSTANCE)
    }

    @Test
    fun fixResolvesToIntegerFunctionCall() {
        assertCastFunctionCall("DIM f# AS DOUBLE : DIM h% AS INTEGER : LET h% = fix(f#)", "fix", I64.INSTANCE)
    }

    @Test
    fun intResolvesToIntegerFunctionCall() {
        assertCastFunctionCall("DIM f# AS DOUBLE : DIM h% AS INTEGER : LET h% = int(f#)", "int", I64.INSTANCE)
    }

    @Test
    fun cdblResolvesToFloatFunctionCall() {
        assertCastFunctionCall("DIM a% AS INTEGER : DIM g# AS DOUBLE : LET g# = cdbl(a%)", "cdbl", F64.INSTANCE)
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    /** The expected explicit integer→float cast. */
    private fun castToFloat(expression: Expression) = CastToF64Expression(0, 0, expression)

    /** The expected explicit float→integer cast: truncation of a half-to-even round. */
    private fun castToInt(expression: Expression) = CastToI64Expression(0, 0, RoundExpression(expression, LF_ROUNDEVEN_F64))

    /** Parses a single program and returns the right-hand side of its (last) assignment statement. */
    private fun rhsOf(text: String): Expression {
        val program = parse(text)
        val assignStatement = program.statements.filterIsInstance<AssignStatement>().last()
        return assignStatement.rhsExpression
    }

    private fun assertCastFunctionCall(text: String, name: String, expectedType: se.dykstrom.jcc.common.types.Type) {
        val fce = rhsOf(text) as FunctionCallExpression
        // Not inlined to a cast node: still an ordinary function call
        assertNotCast(fce)
        assertEquals(name, fce.identifier.name())
        assertEquals(expectedType, typeManager.getType(fce))
    }

    private fun assertNotCast(expression: Expression?) {
        assertNotNull(expression)
        assertFalse(expression is CastToFloatExpression, "unexpected CastToFloatExpression: $expression")
        assertFalse(expression is CastToIntExpression, "unexpected CastToIntExpression: $expression")
        assertFalse(expression is RoundExpression, "unexpected RoundExpression: $expression")
    }
}
