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

package se.dykstrom.jcc.col.compiler

import org.junit.jupiter.api.Test

class ColSemanticsParserBecomeTests : AbstractColSemanticsParserTests() {

    // --- Accepted cases ---

    @Test
    fun shouldAcceptSelfTailRecursion() {
        parse("fun f(n as i64, acc as i64) -> i64 := if n <= 0 then acc else become f(n - 1, acc + 1)")
    }

    @Test
    fun shouldAcceptCrossOverloadTailCall() {
        parse(
            """
            fun fac(n as i64) -> i64 := become fac(n, 1)
            fun fac(n as i64, r as i64) -> i64 := if n <= 1 then r else become fac(n - 1, n * r)
            """
        )
    }

    @Test
    fun shouldAcceptMutualTailRecursion() {
        parse(
            """
            fun even(n as i64) -> bool := if n == 0 then true else become odd(n - 1)
            fun odd(n as i64) -> bool := if n == 0 then false else become even(n - 1)
            """
        )
    }

    @Test
    fun shouldAcceptBecomeUnderNestedTailIf() {
        parse(
            """
            fun f(n as i64) -> i64 := 
                if n > 0 then (if n > 10 then become f(n - 10) else become f(n - 1)) else 0
            """
        )
    }

    @Test
    fun shouldAcceptBecomeWithExactReturnType() {
        parse(
            """
            fun g(n as i64) -> i32 := i32(n)
            fun f(n as i64) -> i32 := become g(n)
            """
        )
    }

    // --- Rejected cases ---

    @Test
    fun shouldRejectBecomeNotInTailPositionOfOperator() {
        parseAndExpectError(
            "fun f(n as i64) -> i64 := if n <= 1 then 1 else n * become f(n - 1)",
            "become is not in tail position: its result is used by '*'"
        )
    }

    @Test
    fun shouldRejectBecomeInIfCondition() {
        parseAndExpectError(
            "fun f(n as i64) -> bool := if become f(n) then true else false",
            "become is not in tail position: its result is used by the condition of an if-expression"
        )
    }

    @Test
    fun shouldRejectBecomeAsCallArgument() {
        parseAndExpectError(
            "fun f(n as i64) -> i64 := f(become f(n))",
            "become is not in tail position: its result is used by a function-call argument"
        )
    }

    @Test
    fun shouldRejectBecomeAsExplicitCastArgument() {
        parseAndExpectError(
            """
            fun g(n as i64) -> i32 := i32(n)
            fun f(n as i64) -> i64 := i64(become g(n))
            """,
            "become is not in tail position: its result is used by a function-call argument"
        )
    }

    @Test
    fun shouldRejectBecomeWithWideningReturnType() {
        parseAndExpectError(
            """
            fun g(n as i64) -> i32 := i32(n)
            fun f(n as i64) -> i64 := become g(n)
            """,
            "tail call returns i32 but function 'f' returns i64; the implicit widening would run after the call"
        )
    }

    @Test
    fun shouldRejectBecomeToExternalFunction() {
        parseAndExpectError(
            "fun f(n as i64) -> i32 := become println(n)",
            "become can only tail-call a user-defined function, not 'println' which is an external or built-in function"
        )
    }

    @Test
    fun shouldRejectTopLevelBecomeInValInitializer() {
        parseAndExpectError(
            """
            fun f(n as i64) -> i64 := f(n)
            val x := become f(5)
            """,
            "become is only allowed inside a function body"
        )
    }

    @Test
    fun shouldRejectTopLevelBecomeInCallArgument() {
        parseAndExpectError(
            """
            fun f(n as i64) -> i64 := f(n)
            call println(become f(5))
            """,
            "become is only allowed inside a function body"
        )
    }
}
