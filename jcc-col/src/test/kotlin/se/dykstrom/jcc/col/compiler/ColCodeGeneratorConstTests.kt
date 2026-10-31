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

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.optimization.ColAstOptimizer
import se.dykstrom.jcc.col.type.ColTypeManager
import se.dykstrom.jcc.common.error.CompilationErrorListener
import se.dykstrom.jcc.common.utils.OptimizationOptions
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Path

/**
 * Compiles const programs from source, at -O0 and -O1, since the AST optimizer only runs at -O1.
 */
class ColCodeGeneratorConstTests {

    @AfterEach
    fun tearDown() {
        OptimizationOptions.INSTANCE.level = 0
    }

    @Test
    fun shouldEmitOneGlobalPerConst() = LEVELS.forEach { level ->
        val text = compile(
            level,
            """
            type Meters as f64
            type Name as string
            const A := 17i32
            const B := 17
            const C := 0.1f32
            const D := 1.5
            const E := true
            const F := "hello"
            const G := Name("Bob")
            const H := Meters(7.3) + Meters(2.2)
            call println(i64(A) + B)
            call println(f64(C) + D)
            call println(E)
            call println(F + string(G))
            call println(string(H))
            """
        )
        assertContains(
            text,
            "@_A = private constant i32 17",
            "@_B = private constant i64 17",
            "@_C = private constant float 0.10000000149011612",
            "@_D = private constant double 1.5",
            "@_E = private constant i1 1",
            "@_F = private constant [6 x i8] c\"hello\\00\"",
            "@_G = private constant [4 x i8] c\"Bob\\00\"",
            "@_H = private constant double 9.5",
        )
    }

    @Test
    fun shouldLoadNumericConstInFunction() = LEVELS.forEach { level ->
        val text = compile(
            level,
            """
            fun f() -> i64 := N + 1
            fun positive() -> bool := N > 0
            const N := 17
            call println(f())
            call println(positive())
            """
        )
        assertContains(text, "@_N = private constant i64 17", "load i64, ptr @_N")
        // The comparison is not folded into an integer literal of the wrong type
        assertFalse(text.contains("ret i64 -1"), text)
    }

    @Test
    fun shouldUseAddressOfStringConst() = LEVELS.forEach { level ->
        val text = compile(
            level,
            """
            const S := "hello"
            call println(S)
            """
        )
        assertContains(text, "@printf(ptr @_.printf.fmt.Str.nl, ptr @_S)")
        assertFalse(text.contains("load ptr, ptr @_S"), text)
    }

    @Test
    fun shouldNotReplaceParameterThatShadowsConst() = LEVELS.forEach { level ->
        val text = compile(
            level,
            """
            const N := 17
            fun f(N as i64) -> i64 := N + 1
            call println(f(1))
            call println(N)
            """
        )
        val body = text.substringAfter("define tailcc i64 @f_I64").substringBefore("}")
        assertFalse(body.contains("17"), body)
        assertFalse(body.contains("@_N"), body)
    }

    private companion object {
        val LEVELS = listOf(0, 1)
    }

    private fun assertContains(text: String, vararg lines: String) {
        lines.forEach { assertTrue(text.contains(it), "missing line: $it\n$text") }
    }

    private fun compile(level: Int, source: String): String {
        OptimizationOptions.INSTANCE.level = level
        val errorListener = CompilationErrorListener()
        val types = ColTypeManager()
        val symbols = ColSymbols()
        val syntaxParser = ColSyntaxParser(errorListener)
        val semanticsParser = ColSemanticsParser(errorListener, symbols, types)
        val optimizer = ColAstOptimizer(types, symbols)
        val codeGenerator = ColCodeGenerator(types, symbols, optimizer)

        val bytes = source.trimIndent().toByteArray(StandardCharsets.UTF_8)
        val program = semanticsParser.parse(syntaxParser.parse(ByteArrayInputStream(bytes)))
        assertEquals(0, errorListener.errors.size, errorListener.errors.toString())
        return codeGenerator.generate(optimizer.program(program).withSourcePath(Path.of("file.col"))).toText()
    }
}
