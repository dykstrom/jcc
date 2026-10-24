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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.main.Language.COL

/**
 * Compile-and-run integration tests for COL opaque types.
 */
class ColCompileAndRunOpaqueTypeIT : AbstractIntegrationTests() {

    @Test
    fun shouldRunProgramWithOpaqueTypes() {
        val source = listOf(
            "type Meters as f64",
            "type Feet as f64",
            "type UserId as i64",
            "type OrderId as i64",
            "type Name as string",
            "fun ascend(from as Meters, by as Feet) -> Meters := Meters(f64(from) + f64(by) * 0.3048)",
            """fun describe(user as UserId, order as OrderId) -> string := "user " + string(i64(user)) + " order " + string(i64(order))""",
            "fun kind(m as Meters) -> f64 := f64(m) * 2.0",
            "fun kind(x as f64) -> f64 := x * 3.0",
            """fun hello(n as Name) -> Name := Name("hello " + string(n))""",
            "val base := Meters(3.0)",
            "val climb as Feet := Feet(10.0)",
            "call println(f64(ascend(base, climb)))",
            "call println(describe(UserId(7), OrderId(1001i32)))",
            "call println(kind(base))",
            "call println(kind(3.0))",
            """call println(string(hello(Name("world"))))""",
        )
        compileAndRun(
            COL, source, listOf(
                "6.048000",
                "user 7 order 1001",
                "6.000000",
                "9.000000",
                "hello world",
            )
        )
    }

    @Test
    fun shouldRunInheritedOperators() {
        val source = listOf(
            "type Meters as f64",
            "type Id as i64",
            "type Name as string",
            "type Flag as bool",
            "val a := Meters(1.5)",
            "val b := Meters(2.0)",
            "val i := Id(7)",
            "val j := Id(3)",
            "call println(f64(a + b))",
            "call println(f64(a - b))",
            "call println(i64(i + j))",
            "call println(i64(i - j))",
            "call println(a == b)",
            "call println(a != b)",
            "call println(a < b)",
            "call println(a <= b)",
            "call println(a > b)",
            "call println(a >= b)",
            "call println(i == j)",
            "call println(i != j)",
            "call println(i < j)",
            "call println(i <= j)",
            "call println(i > j)",
            "call println(i >= j)",
            """call println(string(Name("ab") + Name("cd")))""",
            """call println(Name("x") == Name("x"))""",
            """call println(Name("x") != Name("x"))""",
            "call println(Flag(true) == Flag(false))",
            "call println(Flag(true) != Flag(false))",
        )
        compileAndRun(
            COL, source, listOf(
                "3.500000",
                "-0.500000",
                "10",
                "4",
                "false",
                "true",
                "true",
                "true",
                "false",
                "false",
                "false",
                "true",
                "false",
                "false",
                "true",
                "true",
                "abcd",
                "true",
                "false",
                "false",
                "true",
            )
        )
    }

    @Test
    fun shouldConvertOpaqueValuesToString() {
        // A negative i32 shows that the widening to i64 is a sign extension
        val source = listOf(
            "type Count as i32",
            "type Id as i64",
            "type Ratio as f32",
            "type Meters as f64",
            "type Flag as bool",
            "type Name as string",
            "call println(string(Count(-17i32)))",
            "call println(string(Id(42)))",
            "call println(string(Ratio(1.5f32)))",
            "call println(string(Meters(3.14)))",
            "call println(string(Flag(false)))",
            """call println(string(Name("x")))""",
            """call println("climbed " + string(Meters(2.5)) + " m: " + string(Flag(true)))""",
        )
        compileAndRun(
            COL, source, listOf(
                "-17",
                "42",
                "1.500000",
                "3.140000",
                "false",
                "x",
                "climbed 2.500000 m: true",
            )
        )
    }

    @Test
    fun shouldKeepConcatenatedOpaqueStringsAliveUnderCollection() {
        val source = listOf(
            "type Name as string",
            """fun join(a as Name, b as Name) -> Name := a + Name(" ") + b""",
            "val first := Name(readln())",
            "val second := Name(readln())",
            "val both := join(first, second)",
            "val twice := both + both",
            "call println(string(twice))",
            "call println(both == join(first, second))",
        )
        val output = compileAndRunReturningOutput(
            COL, source, listOf("alpha", "beta"), "-print-gc", "-initial-gc-threshold", "1"
        )
        assertTrue(output.contains("jcc_gc: collect"), "No collection happened: $output")
        val lines = output.lines().filterNot { it.startsWith("jcc_gc:") }.filter { it.isNotEmpty() }
        assertEquals(listOf("alpha betaalpha beta", "true"), lines)
    }

    @Test
    fun shouldRunProgramWithOpaqueFunctionTypes() {
        val source = listOf(
            "type Cmp as (i64, i64) -> i64",
            "type Meters as f64",
            "type Scale as (Meters) -> Meters",
            "fun larger(a as i64, b as i64) -> i64 := if a > b then a else b",
            "fun larger(a as f64, b as f64) -> f64 := if a > b then a else b",
            "fun smaller(a as i64, b as i64) -> i64 := if a < b then a else b",
            "fun double(m as Meters) -> Meters := m + m",
            "fun apply(c as Cmp, a as i64, b as i64) -> i64 := c(a, b)",
            "fun pick(n as i64) -> Cmp := if n <= 0 then Cmp(smaller) else become pick(n - 1)",
            "val c := Cmp(larger)",
            "val s := Scale(double)",
            "call println(apply(c, 3, 8))",
            "call println(c(9, 4))",
            "call println(apply(Cmp(fun(a as i64, b as i64) := a * b), 6, 7))",
            "call println(apply(pick(100_000), 3, 8))",
            "call println(f64(s(Meters(1.25))))",
        )
        compileAndRun(COL, source, listOf("8", "9", "42", "3", "2.500000"))
    }

    @Test
    fun shouldKeepOpaqueStringsAliveUnderCollection() {
        // With a threshold of 1, the collector runs at every allocation, so any opaque string that
        // is not rooted is freed while still in use
        val source = listOf(
            "type Name as string",
            """fun shout(n as Name) -> Name := Name(string(n) + "!")""",
            """fun pair(a as Name, b as Name) -> string := string(shout(a)) + " " + string(shout(b))""",
            "val first := Name(readln())",
            "val second := Name(readln())",
            "val third := shout(first)",
            "call println(pair(first, second))",
            "call println(string(third))",
            "call println(string(first) + string(second))",
        )
        val output = compileAndRunReturningOutput(
            COL, source, listOf("alpha", "beta"), "-print-gc", "-initial-gc-threshold", "1"
        )
        assertTrue(output.contains("jcc_gc: collect"), "No collection happened: $output")
        val lines = output.lines().filterNot { it.startsWith("jcc_gc:") }.filter { it.isNotEmpty() }
        assertEquals(listOf("alpha! beta!", "alpha!", "alphabeta"), lines)
    }
}
