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

package se.dykstrom.jcc.basic.compiler

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.common.utils.GcOptions

/**
 * Tests the garbage-collector plumbing emitted by the LLVM backend (issue #63 phase 2):
 * the jcc_gc_init call injected at the start of main, and the temporary in-module stub
 * definitions that stand in for the not-yet-existing runtime. Only jcc_gc_init is emitted in
 * this phase; roots, frames, and registration arrive in later phases.
 *
 * These are IR-only tests (no clang). The GC options are a JVM-wide singleton, so each test
 * sets them explicitly and the original values are restored afterwards.
 */
internal class BasicLlvmCodeGeneratorGcTests : AbstractBasicCodeGeneratorTests() {

    private val cg = BasicLlvmCodeGenerator(typeManager, symbols, optimizer)

    private var savedPrintGc = false
    private var savedThreshold = 0

    @BeforeEach
    fun setUp() {
        savedPrintGc = GcOptions.INSTANCE.isPrintGc
        savedThreshold = GcOptions.INSTANCE.initialGcThreshold
    }

    @AfterEach
    fun tearDown() {
        GcOptions.INSTANCE.isPrintGc = savedPrintGc
        GcOptions.INSTANCE.initialGcThreshold = savedThreshold
    }

    @Test
    fun shouldInitializeGcAtStartOfMain() {
        GcOptions.INSTANCE.isPrintGc = false
        GcOptions.INSTANCE.initialGcThreshold = 100

        val result = assembleProgram(cg, emptyList())

        // jcc_gc_init is called first in main, with the threshold and flags=0 (debug off)
        assertContains(result, listOf(
            "entry:",
            "call void @jcc_gc_init(i64 100, i64 0)",
        ))
        // The runtime does not exist yet, so jcc_gc_init is given a stub definition, not a declare
        assertContains(result, listOf("define void @jcc_gc_init(i64 %0, i64 %1) {"))
        assertNotContains(result, listOf("declare void @jcc_gc_init"))
    }

    @Test
    fun shouldPassThresholdFromOptions() {
        GcOptions.INSTANCE.isPrintGc = false
        GcOptions.INSTANCE.initialGcThreshold = 5

        val result = assembleProgram(cg, emptyList())

        assertContains(result, listOf("call void @jcc_gc_init(i64 5, i64 0)"))
    }

    @Test
    fun shouldEnableDebugFlagAndLogFromStubWhenPrintGc() {
        GcOptions.INSTANCE.isPrintGc = true
        GcOptions.INSTANCE.initialGcThreshold = 100

        val result = assembleProgram(cg, emptyList())

        // The JCC_GC_DEBUG flag (1) is passed to jcc_gc_init
        assertContains(result, listOf("call void @jcc_gc_init(i64 100, i64 1)"))
        // The stub logs a fixed message via puts
        assertContains(result, listOf(
            "declare i32 @puts(ptr)",
            "@.str.gc.init = private constant [18 x i8] c\"jcc_gc: stub init\\00\"",
            "call i32 @puts(ptr @.str.gc.init)",
        ))
    }

    @Test
    fun shouldNotLogFromStubWhenNotPrintGc() {
        GcOptions.INSTANCE.isPrintGc = false
        GcOptions.INSTANCE.initialGcThreshold = 100

        val result = assembleProgram(cg, emptyList())

        assertNotContains(result, listOf("@puts", "jcc_gc: stub"))
    }

    @Test
    fun shouldNotEmitRootsFramesOrRegistrationYet() {
        // Phase 2 keeps semantics unchanged: no roots, frames, or registration are emitted;
        // those belong to phases 3 and 4.
        GcOptions.INSTANCE.isPrintGc = false
        GcOptions.INSTANCE.initialGcThreshold = 100

        val result = assembleProgram(cg, emptyList())

        assertNotContains(result, listOf(
            "jcc_gc_push_frame",
            "jcc_gc_pop_frame",
            "jcc_gc_add_root",
            "jcc_gc_register",
            "jcc_gc_set_global_roots",
        ))
    }
}
