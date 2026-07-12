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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.basic.BasicTests.Companion.FUN_STR_TO_STR
import se.dykstrom.jcc.basic.BasicTests.Companion.IDENT_STR_S
import se.dykstrom.jcc.basic.BasicTests.Companion.INE_STR_S
import se.dykstrom.jcc.basic.BasicTests.Companion.SL_BAR
import se.dykstrom.jcc.basic.BasicTests.Companion.SL_FOO
import se.dykstrom.jcc.basic.ast.statement.LineInputStatement
import se.dykstrom.jcc.basic.ast.statement.PrintStatement
import se.dykstrom.jcc.common.ast.AddExpression
import se.dykstrom.jcc.common.ast.ArrayDeclaration
import se.dykstrom.jcc.common.ast.AssignStatement
import se.dykstrom.jcc.common.ast.Declaration
import se.dykstrom.jcc.common.ast.FunctionCallExpression
import se.dykstrom.jcc.common.ast.FunctionDefinitionStatement
import se.dykstrom.jcc.common.ast.IntegerLiteral
import se.dykstrom.jcc.common.ast.VariableDeclarationStatement
import se.dykstrom.jcc.common.functions.UserDefinedFunction
import se.dykstrom.jcc.common.symbols.Scope.GLOBAL
import se.dykstrom.jcc.common.types.Arr
import se.dykstrom.jcc.common.types.Identifier
import se.dykstrom.jcc.common.types.Str
import se.dykstrom.jcc.common.utils.GcOptions

/**
 * Tests the garbage-collector plumbing emitted by the LLVM backend (issue #63 phases 3-4):
 * the shadow-stack frames pushed/popped around every function, the roots for string
 * parameters and global variables, the {@code @jcc.gc.global.roots} table, the initialization
 * sequence in main (phase 3), and registration (phase 4) - {@code jcc_gc_register} for
 * freshly-allocated string results, rooted in synthetic {@code .gc.slot.N} locals, and the
 * protect (root-only) path for user-defined function results. The {@code jcc_gc_*} symbols
 * still resolve to the temporary in-module stubs until phase 5.
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
        val result = assembleProgram(cg, emptyList())

        // The three calls are all present in main...
        assertContains(result, listOf(
            "entry:",
            "call void @jcc_gc_init(i64 100, i64 0)",
            "call void @jcc_gc_set_global_roots(ptr @jcc.gc.global.roots)",
            "call void @jcc_gc_push_frame()",
        ))
        // ...and in that order: init must be the first jcc_gc_* call (jcc_gc.h contract). The
        // full call strings only occur at the call site in main, not in the stub definitions.
        val text = result.toText()
        val init = text.indexOf("call void @jcc_gc_init(i64 100, i64 0)")
        val setRoots = text.indexOf("call void @jcc_gc_set_global_roots(ptr @jcc.gc.global.roots)")
        val pushFrame = text.indexOf("call void @jcc_gc_push_frame()")
        assertTrue(init < setRoots)
        assertTrue(setRoots < pushFrame)
        // The runtime does not exist yet, so the GC functions are stubbed, not declared.
        assertContains(result, listOf("define void @jcc_gc_init(i64 %0, i64 %1) {"))
        assertNotContains(result, listOf("declare void @jcc_gc_init"))
    }

    @Test
    fun shouldPopFrameBeforeMainReturns() {
        val result = assembleProgram(cg, emptyList())

        val text = result.toText()
        assertContains(result, listOf("call void @jcc_gc_pop_frame()"))
        // pop_frame comes before the trailing "ret i32 0" (the full call string only occurs at
        // the call site in main, not in the stub definition).
        assertTrue(text.indexOf("call void @jcc_gc_pop_frame()") < text.indexOf("ret i32 0"))
    }

    @Test
    fun shouldPassThresholdFromOptions() {
        GcOptions.INSTANCE.initialGcThreshold = 5

        val result = assembleProgram(cg, emptyList())

        assertContains(result, listOf("call void @jcc_gc_init(i64 5, i64 0)"))
    }

    @Test
    fun shouldEnableDebugFlagAndLogFromStubWhenPrintGc() {
        GcOptions.INSTANCE.isPrintGc = true

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
    fun shouldEmitTerminatorOnlyRootsTableForEmptyProgram() {
        val result = assembleProgram(cg, emptyList())

        // No string globals, so the table holds just the null terminator.
        assertContains(result, listOf(
            "@jcc.gc.global.roots = private global [1 x { ptr, i64 }] [{ ptr, i64 } { ptr null, i64 0 }]",
        ))
    }

    @Test
    fun shouldEmitGlobalRootRangeForStringVariable() {
        // s$ = "foo"  ->  s$ is a global string variable, one root slot of count 1.
        val result = assembleProgram(cg, listOf(AssignStatement(INE_STR_S, SL_FOO)))

        assertContains(result, listOf(
            "@jcc.gc.global.roots = private global [2 x { ptr, i64 }] " +
                    "[{ ptr, i64 } { ptr @_s.do, i64 1 }, { ptr, i64 } { ptr null, i64 0 }]",
        ))
    }

    @Test
    fun shouldEmitGlobalRootRangeForStringArray() {
        // dim sa$(3)  ->  a string array's element region is one range spanning all elements.
        val identStrArr = Identifier("sa$", Arr.from(1, Str.INSTANCE))
        val dim = VariableDeclarationStatement(
            0, 0,
            listOf(ArrayDeclaration(0, 0, identStrArr.name(), identStrArr.type() as Arr, listOf(IntegerLiteral(0, 0, 3)))),
            GLOBAL
        )

        val result = assembleProgram(cg, listOf(dim))

        assertContains(result, listOf("{ ptr, i64 } { ptr @_sa.do_arr, i64 3 }"))
    }

    @Test
    fun shouldPushFrameAndRootStringParameterInUserFunction() {
        // FNid$(x$) = "foo"  ->  the string parameter x$ is rooted in the callee's own frame.
        val ident = Identifier("FNid$", FUN_STR_TO_STR)
        val declarations = listOf(Declaration(0, 0, "x$", Str.INSTANCE))
        val fds = FunctionDefinitionStatement(0, 0, ident, declarations, SL_FOO)

        val result = assembleProgram(cg, listOf(fds))

        assertContains(result, listOf(
            "call void @jcc_gc_push_frame()",
            "call void @jcc_gc_add_root(ptr %_x.do)",
            "call void @jcc_gc_pop_frame()",
        ))
    }

    @Test
    fun shouldNotRegisterPlainStringAssignment() {
        // s$ = "foo"  ->  storing a string literal allocates nothing, so there is no registration.
        val result = assembleProgram(cg, listOf(AssignStatement(INE_STR_S, SL_FOO)))

        assertNotContains(result, listOf("jcc_gc_register"))
    }

    @Test
    fun shouldRegisterAndRootFreshlyAllocatedResult() {
        // PRINT "foo" + "bar"  ->  the concatenation is a fresh allocation: register it, then root
        // it in a synthetic .gc.slot.0 that the prologue allocates, null-inits, and adds as a root.
        val result = assembleProgram(cg, listOf(PrintStatement(listOf(AddExpression(SL_FOO, SL_BAR)))))

        assertContains(result, listOf(
            "%0 = call ptr @add_Str_Str(ptr @_.str.0, ptr @_.str.1)",
            "%1 = call ptr @jcc_gc_register(ptr %0)",
            "store ptr %1, ptr %_.gc.slot.0",
            // The synthetic slot is allocated, null-initialized, and rooted by the prologue.
            "%_.gc.slot.0 = alloca ptr",
            "store ptr null, ptr %_.gc.slot.0",
            "call void @jcc_gc_add_root(ptr %_.gc.slot.0)",
        ))
    }

    @Test
    fun shouldProtectUserFunctionResultWithoutRegistering() {
        // PRINT FNid$("foo") where DEF FNid$(x$) = x$  ->  the user function registered its own
        // result in the callee, so the call site only roots it (protect) - no second register.
        val function = UserDefinedFunction("FNid$", listOf("x"), listOf(Str.INSTANCE), Str.INSTANCE)
        val call = FunctionCallExpression(function.identifier, listOf(SL_FOO), function)

        val result = assembleProgram(cg, listOf(PrintStatement(listOf(call))))

        // The result is rooted in a synthetic slot...
        assertContains(result, listOf("store ptr %0, ptr %_.gc.slot.0"))
        // ...but this identity function allocates nothing, so no jcc_gc_register is emitted at all.
        assertNotContains(result, listOf("jcc_gc_register"))
    }

    @Test
    fun shouldRegisterLineInputResultIntoRootedVariableWithoutSlot() {
        // LINE INPUT s$  ->  the read line is registered, then stored straight into the already
        // rooted global s$; no synthetic slot is needed for this register-into-destination path.
        val result = assembleProgram(cg, listOf(LineInputStatement.builder(IDENT_STR_S).build()))

        assertContains(result, listOf(
            "%0 = call ptr @read_line()",
            "%1 = call ptr @jcc_gc_register(ptr %0)",
            "store ptr %1, ptr @_s.do",
        ))
        // The register-into-rooted-destination variant creates no .gc.slot.
        assertNotContains(result, listOf(".gc.slot"))
    }
}
