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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ColTests.Companion.FUN_ABS
import se.dykstrom.jcc.col.ColTests.Companion.IL_5
import se.dykstrom.jcc.col.ast.expression.BecomeExpression
import se.dykstrom.jcc.col.compiler.ColSemanticsParser
import se.dykstrom.jcc.col.compiler.ColSymbols
import se.dykstrom.jcc.col.type.ColTypeManager
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.CastToIntExpression
import se.dykstrom.jcc.common.ast.Expression
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.IfExpression
import se.dykstrom.jcc.common.ast.MulExpression
import se.dykstrom.jcc.common.ast.NegateExpression
import se.dykstrom.jcc.common.ast.ShiftLeftExpression
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.functions.Function
import se.dykstrom.jcc.common.functions.UserDefinedFunction
import se.dykstrom.jcc.common.types.Bool
import se.dykstrom.jcc.common.types.I32
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Type

/**
 * Unit tests of [TailPositionValidator]. The validator is driven directly, with hand-built
 * expressions standing in for function bodies, so that each branch of the walk can be reached
 * on its own — including combinations the COL grammar makes awkward to write in source.
 */
class TailPositionValidatorTests {

    private val errorListener = CompilationErrorListener()
    private val semanticsParser = ColSemanticsParser(errorListener, ColSymbols(), ColTypeManager())

    /** Validates [body] as the body of a function named 'f' returning [returnType]. */
    private fun check(body: Expression, returnType: Type = I64.INSTANCE) =
        TailPositionValidator(semanticsParser, "function 'f'", returnType).check(body)

    private val errors get() = errorListener.errors.map { it.msg }

    private fun assertNoErrors() = assertTrue(errors.isEmpty(), "unexpected errors: $errors")

    private fun assertError(vararg texts: String) {
        assertEquals(1, errors.size, "expected exactly one error, found: $errors")
        texts.forEach { assertTrue(errors[0].contains(it), "\nExpected: '$it'\nActual:   '${errors[0]}'") }
    }

    /** A become that tail-calls [function], which returns i64 unless given another return type. */
    private fun become(function: Function = udf(), vararg args: Expression) =
        BecomeExpression(FunctionCallExpression(function.identifier, args.toList(), function))

    /** As [become], but at a source position of its own, for bodies holding more than one. */
    private fun becomeAt(line: Int, column: Int) =
        BecomeExpression(line, column, FunctionCallExpression(udf().identifier, listOf(), udf()))

    private fun udf(returnType: Type = I64.INSTANCE) =
        UserDefinedFunction("g", listOf("n"), listOf(I64.INSTANCE), returnType)

    // --- Tail positions ---

    @Test
    fun shouldAcceptBecomeAsWholeBody() {
        check(become())
        assertNoErrors()
    }

    @Test
    fun shouldAcceptBecomeInBothIfBranches() {
        check(IfExpression(IL_5, become(), become()))
        assertNoErrors()
    }

    @Test
    fun shouldAcceptBecomeUnderNestedTailIf() {
        // Tail position propagates through any depth of if-expressions
        check(IfExpression(IL_5, IfExpression(IL_5, become(), become()), IL_5))
        assertNoErrors()
    }

    @Test
    fun shouldLookThroughImplicitWideningCast() {
        // An implicit cast inserted by if-branch promotion must not hide the become, or the
        // widening error below would be replaced by a misleading "consumed by a cast" message
        check(CastToIntExpression(become(udf(I32.INSTANCE)), I64.INSTANCE))
        assertError("tail call returns i32 but function 'f' returns i64")
    }

    // --- Non-tail positions ---

    @Test
    fun shouldReportBecomeInIfCondition() {
        check(IfExpression(become(), IL_5, IL_5))
        assertError("become is not in tail position", "used by the condition of an if-expression")
    }

    @Test
    fun shouldReportBecomeAsBinaryOperand() {
        check(MulExpression(IL_5, become()))
        assertError("used by '*'")
    }

    @Test
    fun shouldReportBecomeAsUnaryOperand() {
        check(NegateExpression(become()))
        assertError("used by '-'")
    }

    @Test
    fun shouldNameUnmappedOperatorGenerically() {
        // An operator missing from the symbol map falls back to a generic name rather than
        // producing an empty or null one
        check(ShiftLeftExpression(0, 0, IL_5, become()))
        assertError("used by an enclosing operator")
    }

    @Test
    fun shouldReportBecomeAsCallArgument() {
        val println = ColSymbols().getFunction("println", listOf(I64.INSTANCE))
        check(FunctionCallExpression(println.identifier, listOf(become()), println))
        assertError("used by a function-call argument")
    }

    @Test
    fun shouldReportBecomeInArgumentOfTailBecome() {
        // The outer become is in tail position, but arguments are evaluated before the call,
        // so a become among them never is
        check(become(udf(), become()))
        assertError("used by a function-call argument")
    }

    @Test
    fun shouldPropagateConsumerThroughIf() {
        // The if is not in tail position, so neither are its branches, and the error names the
        // operator that consumes the if's result rather than the if itself
        check(AddExpression(IL_5, IfExpression(IL_5, become(), IL_5)))
        assertError("used by '+'")
    }

    // --- Rules that apply to a become in tail position ---

    @Test
    fun shouldReportTailCallToNonUserDefinedFunction() {
        check(become(FUN_ABS, IL_5))
        assertError("become can only tail-call a user-defined function", "not 'abs'")
    }

    @Test
    fun shouldReportTailCallWithDifferingReturnType() {
        check(become(udf(Bool.INSTANCE)))
        assertError("tail call returns bool but function 'f' returns i64")
    }

    @Test
    fun shouldAcceptTailCallWithExactReturnType() {
        check(become(udf(I32.INSTANCE)), I32.INSTANCE)
        assertNoErrors()
    }

    @Test
    fun shouldIgnoreUnresolvedCall() {
        // The call was not resolved, so an error has already been reported elsewhere; reporting
        // a second one here would only add noise
        check(BecomeExpression(FunctionCallExpression(udf().identifier, listOf(), null)))
        assertNoErrors()
    }

    // --- Bodies with nothing to validate ---

    @Test
    fun shouldReportEveryBecomeInBody() {
        // Each operand gets its own position, as two becomes in real source would: identical
        // messages at one position are reported once (see CompilationErrorListener)
        check(MulExpression(becomeAt(1, 10), becomeAt(1, 30)))
        assertEquals(2, errors.size, "expected two errors, found: $errors")
    }

    @Test
    fun shouldIgnoreBodyWithoutBecome() {
        check(IfExpression(IL_5, AddExpression(IL_5, IL_5), NegateExpression(IL_5)))
        assertNoErrors()
    }
}
