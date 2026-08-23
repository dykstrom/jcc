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

package se.dykstrom.jcc.main

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.main.Language.COL

/**
 * Compile-and-run integration tests for COL, specifically for testing
 * anonymous functions.
 *
 * @author Johan Dykstrom
 */
@Tag("LLVM")
class ColCompileAndRunAnonymousFunctionsIT : AbstractIntegrationTests() {

    @Test
    fun shouldPassAnonymousFunctionAsArgument() {
        val source = listOf(
            "fun apply(f as (i64, i64) -> i64, x as i64, y as i64) -> i64 := f(x, y)",
            "call println(apply(fun(a as i64, b as i64) -> i64 := a + b, 5, 2))",
            "call println(apply(fun(a as i64, b as i64) := a - b, 5, 2))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, COL)
        runAndAssertSuccess(listOf(), listOf(
            "7",
            "3",
        ))
    }

    @Test
    fun shouldBindAnonymousFunctionToVal() {
        val source = listOf(
            "val add := fun(a as i64, b as i64) := a + b",
            "val half as (f64) -> f64 := fun(x as f64) := x / 2.0",
            "call println(add(5, 2))",
            "call println(half(7.0))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, COL)
        runAndAssertSuccess(listOf(), listOf(
            "7",
            "3.500000",
        ))
    }

    @Test
    fun shouldReturnAnonymousFunctionFromFunction() {
        val source = listOf(
            "fun successor() -> (i64) -> i64 := fun(x as i64) := x + 1",
            "val inc := successor()",
            "call println(inc(41))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, COL)
        runAndAssertSuccess(listOf(), listOf(
            "42",
        ))
    }

    @Test
    fun shouldCallAnonymousFunctionFromFunctionBody() {
        val source = listOf(
            "fun twice(f as (i64) -> i64, x as i64) -> i64 := f(f(x))",
            "val square := fun(a as i64) := a * a",
            "call println(twice(square, 3))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, COL)
        runAndAssertSuccess(listOf(), listOf(
            "81",
        ))
    }

    @Test
    fun shouldUseSeveralAnonymousFunctions() {
        val source = listOf(
            "fun apply(f as (i64) -> i64, x as i64) -> i64 := f(x)",
            "val inc := fun(a as i64) := a + 1",
            "val dec := fun(a as i64) := a - 1",
            "call println(apply(inc, apply(dec, 10)))",
            "call println(apply(fun(a as i64) := a * 3, 7))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, COL)
        runAndAssertSuccess(listOf(), listOf(
            "10",
            "21",
        ))
    }

    @Test
    fun shouldUseBecomeInAnonymousFunction() {
        val source = listOf(
            "fun sum(n as i64, acc as i64) -> i64 := if n == 0 then acc else become sum(n - 1, acc + n)",
            "val total := fun(n as i64) -> i64 := become sum(n, 0)",
            "call println(total(1_000_000))",
        )
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, COL)
        runAndAssertSuccess(listOf(), listOf(
            "500000500000",
        ))
    }
}
