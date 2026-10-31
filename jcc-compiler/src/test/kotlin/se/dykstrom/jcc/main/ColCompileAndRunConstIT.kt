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

import org.junit.jupiter.api.Test
import se.dykstrom.jcc.main.Language.COL

/**
 * Compile-and-run integration tests for COL constants. Each program is compiled both without
 * optimization and at -O1, because the AST optimizer only runs at -O1.
 */
class ColCompileAndRunConstIT : AbstractIntegrationTests() {

    @Test
    fun shouldUseOpaqueConstInFunction() {
        val source = listOf(
            "type Meters as f64",
            "const LENGTH := Meters(7.3) + Meters(2.2)",
            "fun twice() -> Meters := LENGTH + LENGTH",
            "call println(string(twice()))",
        )
        compileAndRunAtEachLevel(source, listOf("19.000000"))
    }

    @Test
    fun shouldUseConstOfEachTypeInFunction() {
        val source = listOf(
            "type Name as string",
            "const A := 17i32",
            "const B := 17",
            "const C := 0.1f32",
            "const D := 1.5",
            "const E := true",
            "const F := \"hello\"",
            "const G := Name(\"Bob\")",
            "const W as i64 := 17i32",
            "fun a() -> i32 := A",
            "fun b() -> i64 := B",
            "fun c() -> f32 := C",
            "fun d() -> f64 := D",
            "fun e() -> bool := E",
            "fun f() -> string := F",
            "fun g() -> Name := G",
            "fun w() -> i64 := W",
            "call println(a())",
            "call println(b())",
            "call println(c())",
            "call println(d())",
            "call println(e())",
            "call println(f())",
            "call println(string(g()))",
            "call println(w())",
        )
        compileAndRunAtEachLevel(source, listOf("17", "17", "0.100000", "1.500000", "true", "hello", "Bob", "17"))
    }

    @Test
    fun shouldUseConstDeclaredBelow() {
        val source = listOf(
            "call println(N)",
            "call println(f())",
            "fun f() -> i64 := N + 1",
            "const N := 17",
        )
        compileAndRunAtEachLevel(source, listOf("17", "18"))
    }

    @Test
    fun shouldDeriveConstFromEarlierConst() {
        val source = listOf(
            "const A := 17",
            "const B := A * 2 + 1",
            "const C := if B > 30 then \"big\" else \"small\"",
            "call println(B)",
            "call println(C)",
        )
        compileAndRunAtEachLevel(source, listOf("35", "big"))
    }

    @Test
    fun shouldFoldToTheRuntimeValue() {
        // Each const is compared with the same expression evaluated at runtime, from parameters
        val source = listOf(
            "fun add32(a as f32, b as f32) -> f32 := a + b",
            "fun mul32(a as f32, b as f32) -> f32 := a * b",
            "fun div32(a as f32, b as f32) -> f32 := a / b",
            "fun addI32(a as i32, b as i32) -> i32 := a + b",
            "fun idiv(a as i64, b as i64) -> i64 := a div b",
            "fun imod(a as i64, b as i64) -> i64 := a mod b",
            "fun toI32(x as f64) -> i32 := i32(x)",
            "fun toF32(x as i64) -> f32 := f32(x)",
            "fun toF64(x as f32) -> f64 := f64(x)",
            "const S := 0.1f32 + 0.2f32",
            "const Q := 0.1f32 * 3.0f32",
            "const V := 1.0f32 / 3.0f32",
            "const O := 2147483647i32 + 1i32",
            "const DV := -7 div 2",
            "const MD := -7 mod 2",
            "const T := i32(-3.9)",
            "const R := f32(16777217)",
            "const X := f64(0.1f32)",
            "call println(S == add32(0.1f32, 0.2f32))",
            "call println(Q == mul32(0.1f32, 3.0f32))",
            "call println(V == div32(1.0f32, 3.0f32))",
            "call println(O == addI32(2147483647i32, 1i32))",
            "call println(DV == idiv(-7, 2))",
            "call println(MD == imod(-7, 2))",
            "call println(T == toI32(-3.9))",
            "call println(R == toF32(16777217))",
            "call println(X == toF64(0.1f32))",
            "call println(O)",
        )
        val expected = List(9) { "true" } + "-2147483648"
        compileAndRunAtEachLevel(source, expected)
    }

    @Test
    fun shouldEvaluateLogicalAndRelationalOperators() {
        val source = listOf(
            "const A := 17",
            "const B := A > 10 and not (A == 18) xor false",
            "const C := 6 & 3 | 8 ^ 1",
            "const Z := 0.0",
            "const INF := 1.0 / Z",
            "fun positive() -> bool := A > 0",
            "call println(B)",
            "call println(C)",
            "call println(INF)",
            "call println(positive())",
        )
        compileAndRunAtEachLevel(source, listOf("true", "11", "inf", "true"))
    }

    @Test
    fun shouldNotEvaluateBranchNotTaken() {
        val source = listOf(
            "const A := 17",
            "const B := 0",
            "const C := if B == 0 then 0 else A div B",
            "call println(C)",
        )
        compileAndRunAtEachLevel(source, listOf("0"))
    }

    @Test
    fun shouldLetParameterShadowConst() {
        val source = listOf(
            "const N := 17",
            "fun f(N as i64) -> i64 := N + 1",
            "call println(f(1))",
            "call println(N)",
        )
        compileAndRunAtEachLevel(source, listOf("2", "17"))
    }

    private fun compileAndRunAtEachLevel(source: List<String>, expectedOutput: List<String>) {
        val sourcePath = createSourceFile(source, COL)
        compileAndAssertSuccess(sourcePath, COL)
        runAndAssertSuccess(listOf(), expectedOutput)
        compileAndAssertSuccess(sourcePath, COL, "-O1")
        runAndAssertSuccess(listOf(), expectedOutput)
    }
}
