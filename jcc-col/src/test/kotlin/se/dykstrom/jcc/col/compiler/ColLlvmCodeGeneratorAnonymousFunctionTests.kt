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
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.code.TargetProgram
import se.dykstrom.jcc.common.error.CompilationErrorListener
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

/**
 * Code generation tests for anonymous functions. Unlike the other code generator tests, these
 * compile from source, since what they pin is the result of lambda lifting, which happens during
 * semantic analysis.
 */
internal class ColLlvmCodeGeneratorAnonymousFunctionTests : AbstractColCodeGeneratorTests() {

    private val errorListener = CompilationErrorListener()
    private val syntaxParser = ColSyntaxParser(errorListener)
    private val semanticsParser = ColSemanticsParser(errorListener, symbols, typeManager)
    @Test
    fun shouldEmitLiftedFunctionForValInitializer() {
        val result = compile(
            """
            val add := fun(a as i64, b as i64) := a + b
            call println(add(5, 2))
            """
        )
        assertContains(result, listOf(
            // The lifted function is emitted once, under its synthesized name
            "define tailcc i64 @lambda.0_I64_I64(i64 %0, i64 %1)",
            "%4 = add i64 %2, %3",
            // The val holds a pointer to it, and the call goes through that pointer
            "store ptr @lambda.0_I64_I64, ptr %_add",
        ))
        assertEquals(1, result.toText().lines().count { it.contains("define tailcc i64 @lambda.0_I64_I64") })
    }

    @Test
    fun shouldEmitLiftedFunctionForArgument() {
        val result = compile(
            """
            fun apply(f as (i64) -> i64, x as i64) -> i64 := f(x)
            call println(apply(fun(a as i64) := a + 1, 5))
            """
        )
        assertContains(result, listOf(
            "define tailcc i64 @lambda.0_I64(i64 %0)",
            // The lambda is passed as a plain function pointer
            $$"call tailcc i64 @apply_FunL$I64$R.toI64_I64(ptr @lambda.0_I64, i64 5)",
        ))
    }

    @Test
    fun shouldEmitDistinctLiftedFunctions() {
        val result = compile(
            """
            val inc := fun(a as i64) := a + 1
            val dec := fun(a as i64) := a - 1
            call println(inc(dec(5)))
            """
        )
        assertContains(result, listOf(
            "define tailcc i64 @lambda.0_I64(i64 %0)",
            "define tailcc i64 @lambda.1_I64(i64 %0)",
        ))
    }

    @Test
    fun shouldEmitLiftedFunctionReturnedFromFunction() {
        val result = compile(
            """
            fun adder() -> (i64) -> i64 := fun(x as i64) := x + 1
            val f := adder()
            call println(f(5))
            """
        )
        assertContains(result, listOf(
            "define tailcc i64 @lambda.0_I64(i64 %0)",
            "define tailcc ptr @adder()",
            "ret ptr @lambda.0_I64",
        ))
    }

    private fun compile(source: String): TargetProgram {
        val bytes = source.trimIndent().toByteArray(StandardCharsets.UTF_8)
        val program = semanticsParser.parse(syntaxParser.parse(ByteArrayInputStream(bytes)))
        return cg.generate(optimizer.program(program).withSourcePath(sourcePath))
    }
}
