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
 * Compiles programs from source at -O1, and checks what the AST optimizer folded.
 */
class ColCodeGeneratorOptimizationTests {

    @AfterEach
    fun tearDown() {
        OptimizationOptions.INSTANCE.level = 0
    }

    @Test
    fun shouldFoldStringConcatenationEverywhere() {
        val text = compile(
            """
            fun branch(c as bool) -> string := if c then "a" + "b" else "x"
            fun tail(n as i64, s as string) -> string := if n <= 0 then s else become tail(n - 1, "c" + "d")
            val v := "e" + "f"
            call println("g" + "h")
            while millis() < 0 do
              call println("i" + "j")
            end
            call println(branch(true))
            call println(tail(1, ""))
            call println(v)
            """
        )
        listOf("ab", "cd", "ef", "gh", "ij").forEach {
            assertTrue(text.contains("c\"$it\\00\""), "missing literal: $it\n$text")
        }
        assertFalse(text.contains("call ptr @col_concat_str_str"), text)
    }

    @Test
    fun shouldReplaceIfExpressionWithSelectedBranch() {
        val text = compile(
            """
            fun count(n as i64) -> i64 := if n <= 0 then 0 else become count(n - 1)
            fun picked(n as i64) -> i64 := if 1 < 2 then become count(n) else 0
            call println(picked(3))
            """
        )
        val body = text.substringAfter("define tailcc i64 @picked_I64").substringBefore("}")
        assertTrue(body.contains("musttail call tailcc i64 @count_I64"), body)
        assertFalse(body.contains("br i1"), body)
    }

    @Test
    fun shouldKeepOpaqueTypeOfConversionOfLiteral() {
        // Folding Meters(1.0) to an f64 literal left the additions without a type
        val text = compile(
            """
            type Meters as f64
            fun add(m as Meters) -> Meters := m + Meters(1.0)
            val m := Meters(2.0)
            call println(f64(Meters(1.0) + m))
            call println(f64(add(m)))
            """
        )
        assertTrue(text.contains("fadd double"), text)
    }

    private fun compile(source: String): String {
        OptimizationOptions.INSTANCE.level = 1
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
