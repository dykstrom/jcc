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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.col.ColTests.Companion.IDE_I64_A
import se.dykstrom.jcc.col.ColTests.Companion.IDE_I64_B
import se.dykstrom.jcc.col.ColTests.Companion.SL_FOO
import se.dykstrom.jcc.col.ast.expression.BecomeExpression
import se.dykstrom.jcc.col.compiler.ColSymbols.BF_PRINTLN_STR
import se.dykstrom.jcc.common.ast.*
import se.dykstrom.jcc.common.ast.IntegerLiteral.ONE
import se.dykstrom.jcc.common.ast.IntegerLiteral.ZERO
import se.dykstrom.jcc.common.functions.UserDefinedFunction
import se.dykstrom.jcc.common.types.Fun
import se.dykstrom.jcc.common.types.I64
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.utils.GcOptions

/**
 * Tests the garbage-collector plumbing COL emits now that it wires [RuntimeGcCodeGenerator]:
 * main's initialization sequence, the shadow-stack frames, and the global-roots table. The string
 * operations that make COL need the collector are tested in [ColLlvmCodeGeneratorStringTests].
 */
internal class ColLlvmCodeGeneratorGcTests : AbstractColCodeGeneratorTests() {

    private val cg = ColLlvmCodeGenerator(typeManager, symbols, optimizer)

    private var savedPrintGc = false
    private var savedThreshold = 0

    @BeforeEach
    fun setUp() {
        savedPrintGc = GcOptions.INSTANCE.isPrintGc
        savedThreshold = GcOptions.INSTANCE.initialGcThreshold
        GcOptions.INSTANCE.isPrintGc = false
        GcOptions.INSTANCE.initialGcThreshold = 100
    }

    @AfterEach
    fun tearDown() {
        GcOptions.INSTANCE.isPrintGc = savedPrintGc
        GcOptions.INSTANCE.initialGcThreshold = savedThreshold
    }

    @Test
    fun shouldInitializeGcThenSetRootsThenPushFrameInMain() {
        val result = assembleProgram(cg, listOf())

        assertContains(result, listOf(
            "entry:",
            "call void @jcc_gc_init(i64 100, i64 0)",
            "call void @jcc_gc_set_global_roots(ptr @jcc.gc.global.roots)",
            "call void @jcc_gc_push_frame()",
        ))
        // In that order: init must be the first jcc_gc_* call (jcc_gc.h contract)
        val text = result.toText()
        val init = text.indexOf("call void @jcc_gc_init(i64 100, i64 0)")
        val setRoots = text.indexOf("call void @jcc_gc_set_global_roots(ptr @jcc.gc.global.roots)")
        val pushFrame = text.indexOf("call void @jcc_gc_push_frame()")
        assertTrue(init < setRoots)
        assertTrue(setRoots < pushFrame)
        // The GC functions are declared, resolving against the real runtime in libjcccol
        assertContains(result, listOf("declare void @jcc_gc_init(i64, i64)"))
        assertNotContains(result, listOf("define void @jcc_gc_init"))
    }

    @Test
    fun shouldPopFrameBeforeMainReturns() {
        val result = assembleProgram(cg, listOf())

        val text = result.toText()
        assertContains(result, listOf("call void @jcc_gc_pop_frame()"))
        assertTrue(text.indexOf("call void @jcc_gc_pop_frame()") < text.indexOf("ret i32 0"))
    }

    @Test
    fun shouldPassThresholdFromOptions() {
        GcOptions.INSTANCE.initialGcThreshold = 5

        val result = assembleProgram(cg, listOf())

        assertContains(result, listOf("call void @jcc_gc_init(i64 5, i64 0)"))
    }

    @Test
    fun shouldEnableDebugFlagWhenPrintGc() {
        GcOptions.INSTANCE.isPrintGc = true

        val result = assembleProgram(cg, listOf())

        // -print-gc passes the JCC_GC_DEBUG flag (1) to jcc_gc_init; the runtime does the logging
        assertContains(result, listOf("call void @jcc_gc_init(i64 100, i64 1)"))
    }

    @Test
    fun shouldEmitTerminatorOnlyRootsTable() {
        // COL vals are locals of main, rooted in its frame, and string literals are constants -
        // so COL has no global roots, and the table main registers holds just the terminator
        val result = assembleProgram(cg, listOf(funCall(BF_PRINTLN_STR, SL_FOO)))

        assertContains(result, listOf(
            "@jcc.gc.global.roots = private global [1 x { ptr, i64 }] [{ ptr, i64 } { ptr null, i64 0 }]"
        ))
    }

    @Test
    fun shouldPushAndPopFrameInUserDefinedFunction() {
        // Given: fun inc(a as i64) -> i64 := a + 1
        val identifier = Identifier("inc", Fun.from(listOf(I64.INSTANCE), I64.INSTANCE))
        val declarations = listOf(Declaration(0, 0, "a", I64.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, AddExpression(IDE_I64_A, ONE))

        val result = assembleProgram(cg, listOf(fds))

        // One frame for inc and one for main, each closed before its own ret
        assertEquals(2, callCount(result.toText(), "call void @jcc_gc_push_frame()"))
        assertEquals(2, callCount(result.toText(), "call void @jcc_gc_pop_frame()"))
        // The collector is initialized in main only
        assertEquals(1, callCount(result.toText(), "call void @jcc_gc_init"))
    }

    @Test
    fun shouldPopFrameOnEveryTailLeafOfBecomeFunction() {
        // Given: fun count(a, b) -> i64 := if a <= 0 then b else become count(a - 1, b + 1)
        // The value leaf is generated by ColFunDefCodeGenerator, bypassing ReturnCodeGenerator, so
        // it has to close the frame itself - otherwise the accumulator idiom, COL's most common
        // become shape, leaks a shadow-stack frame per call.
        val identifier = Identifier("count", Fun.from(listOf(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE))
        val udf = UserDefinedFunction("count", listOf("a", "b"), listOf(I64.INSTANCE, I64.INSTANCE), I64.INSTANCE)
        val declarations = listOf(Declaration(0, 0, "a", I64.INSTANCE), Declaration(0, 0, "b", I64.INSTANCE))
        val tailCall = FunctionCallExpression(
            identifier,
            listOf(SubExpression(IDE_I64_A, ONE), AddExpression(IDE_I64_B, ONE)),
            udf
        )
        val body = IfExpression(LessOrEqualExpression(IDE_I64_A, ZERO), IDE_I64_B, BecomeExpression(tailCall))
        val fds = FunctionDefinitionStatement(0, 0, identifier, declarations, body)

        val result = assembleProgram(cg, listOf(fds))

        // Three pops: count's value leaf, count's become leaf, and main's own frame
        val lines = result.lines().map { it.toText().trim() }
        assertEquals(3, lines.count { it == POP_FRAME })
        // Each pop is immediately followed by the instruction that leaves the function
        lines.forEachIndexed { index, line ->
            if (line == POP_FRAME) {
                val next = lines[index + 1]
                assertTrue(
                    next.startsWith("ret") || next.contains("musttail call"),
                    "pop_frame must be immediately followed by ret or musttail, was: $next"
                )
            }
        }
    }

    private fun callCount(text: String, call: String) = text.split(call).size - 1

    companion object {
        private const val POP_FRAME = "call void @jcc_gc_pop_frame()"
    }
}
