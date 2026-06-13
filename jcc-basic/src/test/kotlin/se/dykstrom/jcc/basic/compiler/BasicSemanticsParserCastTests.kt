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

/**
 * Pins the *current* behaviour of [BasicSemanticsParser] at every site where an implicit numeric
 * cast is accepted: after semantic analysis the AST still contains the bare sub-expression, with
 * NO [CastToFloatExpression] / [CastToIntExpression] / [RoundExpression] wrapper inserted. Today
 * each backend re-derives the conversion during code generation instead.
 *
 * This is the Phase 1 safety net for issue #52. When semantic analysis is changed to make casts
 * explicit (Phase 3), these assertions are expected to flip: the bare nodes become wrapped in
 * cast nodes. Mirrors COL's `ColSemanticsParserCastTests`.
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
    // Implicit cast sites (issue #52 inventory). Each asserts NO cast node yet.
    // ------------------------------------------------------------------------

    @Test
    fun assignmentIntToFloatHasNoCast() {
        // LET f# = a%  : integer -> double, silently accepted, no warning
        val rhs = rhsOf("DIM a% AS INTEGER : DIM f# AS DOUBLE : LET f# = a%")
        assertNotCast(rhs)
        assertEquals(IDE_I64_A, rhs)
    }

    @Test
    fun assignmentFloatToIntHasNoCast() {
        // LET a% = f#  : double -> integer (rounding at code gen), warns
        val rhs = rhsOf("DIM a% AS INTEGER : DIM f# AS DOUBLE : LET a% = f#")
        assertNotCast(rhs)
        assertEquals(IDE_F64_F, rhs)
    }

    @Test
    fun binaryIntPlusFloatHasNoCast() {
        // a% + 3.14 : the integer operand is promoted to double, currently with no cast node
        val rhs = rhsOf("DIM a% AS INTEGER : DIM f# AS DOUBLE : LET f# = a% + 3.14") as AddExpression
        assertNotCast(rhs.left)
        assertEquals(IDE_I64_A, rhs.left)
        assertEquals(FL_3_14, rhs.right)
    }

    @Test
    fun relationalIntVsFloatHasNoCast() {
        // a% > 3.14 : the integer operand is promoted to double for the comparison
        val rhs = rhsOf("DIM a% AS INTEGER : DIM h% AS INTEGER : LET h% = (a% > 3.14)") as GreaterExpression
        assertNotCast(rhs.left)
        assertEquals(IDE_I64_A, rhs.left)
        assertEquals(FL_3_14, rhs.right)
    }

    @Test
    fun functionArgFloatToIntHasNoCast() {
        // sum(f#) : double argument to an integer parameter, warns
        val rhs = rhsOf("DIM f# AS DOUBLE : DIM h% AS INTEGER : LET h% = sum(f#)") as FunctionCallExpression
        assertEquals(1, rhs.args.size)
        assertNotCast(rhs.args[0])
        assertEquals(IDE_F64_F, rhs.args[0])
    }

    @Test
    fun functionReturnFloatToIntHasNoCast() {
        // DEF FNfoo%() = 1.0 : double body for an integer return, warns
        val program = parse("DEF FNfoo%() = 1.0")
        val fds = program.statements[0] as FunctionDefinitionStatement
        assertNotCast(fds.expression())
        assertEquals(FL_1_0, fds.expression())
    }

    @Test
    fun arraySubscriptFloatToIntHasNoCast() {
        // foo(3.14) : float subscript into an integer array, warns
        val rhs = rhsOf("DIM foo(10) AS INTEGER : DIM h% AS INTEGER : LET h% = foo(3.14)") as ArrayAccessExpression
        assertEquals(1, rhs.subscripts.size)
        assertNotCast(rhs.subscripts[0])
        assertEquals(FL_3_14, rhs.subscripts[0])
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

    @Test
    fun sleepIntArgumentHasNoCast() {
        // SLEEP a% : SLEEP takes a double (no float->int, hence no warning); the integer argument
        // is an int->double coercion site, currently left bare in the AST.
        val program = parse("DIM a% AS INTEGER : SLEEP a%")
        val sleep = program.statements.filterIsInstance<SleepStatement>().single()
        assertNotCast(sleep.expression)
        assertEquals(IDE_I64_A, sleep.expression)
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
        assertFalse(expression is CastToF64Expression, "unexpected CastToF64Expression: $expression")
        assertFalse(expression is CastToI64Expression, "unexpected CastToI64Expression: $expression")
        assertFalse(expression is RoundExpression, "unexpected RoundExpression: $expression")
    }
}
