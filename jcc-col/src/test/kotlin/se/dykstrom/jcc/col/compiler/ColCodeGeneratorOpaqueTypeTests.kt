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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.type.ColTypeManager
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.optimization.DefaultAstOptimizer
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

/**
 * Tests that opaque types cost nothing at run time: a program written with opaque types compiles
 * to the same IR as the same program written with the underlying types, apart from comments and
 * the type names in mangled function names. For an opaque type over string, this also means that
 * the collector roots its values exactly as it roots strings.
 */
internal class ColCodeGeneratorOpaqueTypeTests : AbstractColCodeGeneratorTests() {

    @Test
    fun shouldGenerateSameCodeForOpaqueFloatAsForFloat() {
        val opaque = compile(
            """
            type Meters as f64
            fun half(x as Meters) -> Meters := Meters(f64(x) / 2.0)
            val m := Meters(3.0)
            call println(f64(half(m)))
            """
        )
        val plain = compile(
            """
            fun half(x as f64) -> f64 := x / 2.0
            val m := 3.0
            call println(half(m))
            """
        )
        assertEquals(plain, opaque.replace("Opaque.Meters", "F64"))
    }

    @Test
    fun shouldGenerateSameCodeForOpaqueIntegerAsForInteger() {
        val opaque = compile(
            """
            type Id as i64
            fun next(id as Id) -> Id := Id(i64(id) + 1)
            call println(i64(next(Id(7i32))))
            """
        )
        val plain = compile(
            """
            fun next(id as i64) -> i64 := id + 1
            call println(next(7i32))
            """
        )
        assertEquals(plain, opaque.replace("Opaque.Id", "I64"))
    }

    @Test
    fun shouldGenerateSameCodeForOpaqueStringAsForString() {
        val opaque = compile(
            """
            type Name as string
            fun greet(n as Name) -> Name := Name(string(n) + "!")
            val n := Name(readln())
            val g := greet(n)
            call println(string(g))
            """
        )
        val plain = compile(
            """
            fun greet(n as string) -> string := n + "!"
            val n := readln()
            val g := greet(n)
            call println(g)
            """
        )
        assertEquals(plain, opaque.replace("Opaque.Name", "Str"))
        // The parameter, the locals and the call results are all rooted
        assertTrue(opaque.lines().count { it.contains("@jcc_gc_add_root") } >= 4, opaque)
    }

    @Test
    fun shouldGenerateSameCodeForOpaqueFloatOperatorsAsForFloatOperators() {
        val opaque = compile(
            """
            type Meters as f64
            fun f(a as Meters, b as Meters) -> Meters := if a < b then a + b else a - b
            fun g(a as Meters, b as Meters) -> bool := a == b
            call println(f64(f(Meters(1.0), Meters(2.0))))
            call println(g(Meters(1.0), Meters(2.0)))
            """
        )
        val plain = compile(
            """
            fun f(a as f64, b as f64) -> f64 := if a < b then a + b else a - b
            fun g(a as f64, b as f64) -> bool := a == b
            call println(f(1.0, 2.0))
            call println(g(1.0, 2.0))
            """
        )
        assertEquals(plain, opaque.replace("Opaque.Meters", "F64"))
        assertTrue(opaque.contains("fcmp olt"), opaque)
        assertTrue(opaque.contains("fadd"), opaque)
    }

    @Test
    fun shouldGenerateSameCodeForOpaqueIntegerOperatorsAsForIntegerOperators() {
        val opaque = compile(
            """
            type Id as i64
            fun f(a as Id, b as Id) -> Id := if a < b then a + b else a - b
            fun g(a as Id, b as Id) -> bool := a == b
            call println(i64(f(Id(1), Id(2))))
            call println(g(Id(1), Id(2)))
            """
        )
        val plain = compile(
            """
            fun f(a as i64, b as i64) -> i64 := if a < b then a + b else a - b
            fun g(a as i64, b as i64) -> bool := a == b
            call println(f(1, 2))
            call println(g(1, 2))
            """
        )
        assertEquals(plain, opaque.replace("Opaque.Id", "I64"))
        assertTrue(opaque.contains("icmp slt"), opaque)
    }

    @Test
    fun shouldGenerateSameCodeForOpaqueStringOperatorsAsForStringOperators() {
        val opaque = compile(
            """
            type Name as string
            fun f(a as Name, b as Name) -> Name := if a == b then a else a + b
            call println(string(f(Name(readln()), Name(readln()))))
            """
        )
        val plain = compile(
            """
            fun f(a as string, b as string) -> string := if a == b then a else a + b
            call println(f(readln(), readln()))
            """
        )
        assertEquals(plain, opaque.replace("Opaque.Name", "Str"))
        assertTrue(opaque.contains("@col_concat_str_str"), opaque)
        assertTrue(opaque.contains("@strcmp"), opaque)
    }

    @Test
    fun shouldGenerateSameCodeForOpaqueToStringAsForUnderlyingToString() {
        val opaque = compile(
            """
            type A as i32
            type B as i64
            type C as f32
            type D as f64
            type E as bool
            fun a(x as A) -> string := string(x)
            fun b(x as B) -> string := string(x)
            fun c(x as C) -> string := string(x)
            fun d(x as D) -> string := string(x)
            fun e(x as E) -> string := string(x)
            call println(a(A(17i32)) + b(B(17)) + c(C(1.5f32)) + d(D(1.5)) + e(E(true)))
            """
        )
        val plain = compile(
            """
            fun a(x as i32) -> string := string(x)
            fun b(x as i64) -> string := string(x)
            fun c(x as f32) -> string := string(x)
            fun d(x as f64) -> string := string(x)
            fun e(x as bool) -> string := string(x)
            call println(a(17i32) + b(17) + c(1.5f32) + d(1.5) + e(true))
            """
        )
        val renamed = opaque
            .replace("Opaque.A", "I32")
            .replace("Opaque.B", "I64")
            .replace("Opaque.C", "F32")
            .replace("Opaque.D", "F64")
            .replace("Opaque.E", "Bool")
        assertEquals(plain, renamed)
        assertTrue(opaque.contains("sext i32"), opaque)
        assertTrue(opaque.contains("fpext float"), opaque)
        assertTrue(opaque.contains("@col_string_bool"), opaque)
    }

    @Test
    fun shouldGenerateSameCodeForOpaqueFunctionAsForFunction() {
        val opaque = compile(
            """
            type Cmp as (i64, i64) -> i64
            fun larger(a as i64, b as i64) -> i64 := if a > b then a else b
            fun larger(a as f64, b as f64) -> f64 := if a > b then a else b
            fun apply(c as Cmp, a as i64, b as i64) -> i64 := c(a, b)
            val c := Cmp(larger)
            call println(apply(c, 1, 2))
            call println(c(3, 4))
            call println(apply(Cmp(fun(a as i64, b as i64) := a + b), 5, 6))
            """
        )
        val plain = compile(
            """
            fun larger(a as i64, b as i64) -> i64 := if a > b then a else b
            fun larger(a as f64, b as f64) -> f64 := if a > b then a else b
            fun apply(c as (i64, i64) -> i64, a as i64, b as i64) -> i64 := c(a, b)
            val c as (i64, i64) -> i64 := larger
            call println(apply(c, 1, 2))
            call println(c(3, 4))
            call println(apply(fun(a as i64, b as i64) := a + b, 5, 6))
            """
        )
        assertEquals(plain, opaque.replace("Opaque.Cmp", "FunL\$I64.I64\$R.toI64"))
        assertTrue(opaque.contains("call tailcc i64 %"), opaque)
    }

    @Test
    fun shouldGenerateDistinctFunctionsForOpaqueAndUnderlyingOverloads() {
        val ir = compile(
            """
            type I64 as f64
            fun f(x as I64) -> i64 := 1
            fun f(x as i64) -> i64 := 2
            call println(f(I64(1.0)) + f(1))
            """
        )
        assertTrue(ir.contains("@f_Opaque.I64("), ir)
        assertTrue(ir.contains("@f_I64("), ir)
    }

    /**
     * Compiles [source] to IR with a fresh compiler, and removes the comment lines, which quote the
     * source code.
     */
    private fun compile(source: String): String {
        val errorListener = CompilationErrorListener()
        val types = ColTypeManager()
        val symbols = ColSymbols()
        val syntaxParser = ColSyntaxParser(errorListener)
        val semanticsParser = ColSemanticsParser(errorListener, symbols, types)
        val optimizer = DefaultAstOptimizer(types, symbols)
        val codeGenerator = ColCodeGenerator(types, symbols, optimizer)

        val bytes = source.trimIndent().toByteArray(StandardCharsets.UTF_8)
        val program = semanticsParser.parse(syntaxParser.parse(ByteArrayInputStream(bytes)))
        return codeGenerator.generate(optimizer.program(program).withSourcePath(sourcePath))
            .toText()
            .lines()
            .filterNot { it.trim().startsWith(";") }
            .joinToString("\n")
    }
}
