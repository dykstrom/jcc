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

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.main.Language.BASIC

/**
 * Compile-and-run integration tests for the BASIC LLVM garbage collector (issue #63).
 *
 * The backend hands every freshly-allocated string to the collector
 * ([registration][RuntimeGcCodeGenerator]) and roots it, having deleted the old eager frees. This
 * fixes the requirement-4 correctness class: a user-defined function may return an argument it does
 * not own, and the caller may pass a transient (freshly-allocated) string as that argument. The old
 * backend eagerly freed such a transient argument right after the call, so the returned alias became
 * a use-after-free. It is now kept reachable instead.
 *
 * As of phase 5 the `jcc_gc_*` symbols resolve to the real mark-sweep runtime in libjccbas, so these
 * tests also assert that collection actually happens: under `-print-gc` the runtime logs its own
 * `jcc_gc: init:` / `collect:` / `exit:` lines to stdout, and the exit stats show live objects
 * staying bounded well below the number of strings created.
 *
 * @author Johan Dykstrom
 */
@Tag("LLVM")
class BasicGarbageCollectionIT : AbstractIntegrationTests() {

    @Test
    fun shouldNotFreeTransientArgumentReturnedByFunction() {
        // FNid$ returns its argument; the arguments are transient concatenations. Under the old
        // eager-free behaviour the concatenation passed to FNid$ was freed right after the call,
        // leaving the returned value (and anything built from it) dangling. The backend registers
        // and roots the concatenation instead, so every result below is correct.
        val source = listOf(
            """
            DEF FNid$(x AS STRING) = x
            DEF FNcat$(x AS STRING, y AS STRING) = x + y

            PRINT FNid$("Hello, " + "GC!")
            PRINT FNcat$(FNid$("a" + "b"), FNid$("c" + "d"))
            """
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileAndAssertSuccess(sourcePath, BASIC)
        runAndAssertSuccess(
            listOf(),
            listOf(
                "Hello, GC!",
                "abcd",
            )
        )
    }

    @Test
    fun shouldEmitRealRuntimeDebugOutputUnderPrintGc() {
        // With -print-gc the real runtime logs its own lifecycle lines to stdout. Seeing them proves
        // the GC calls the backend emits are linked against the actual collector, not stubs. The old
        // "jcc_gc: stub *" logging is gone.
        val source = listOf(
            """
            PRINT "foo" + "bar"
            """
        )
        val output = compileAndRunReturningOutput(
            BASIC, source, extraArgs = arrayOf("-print-gc", "-initial-gc-threshold", "100")
        )
        assertTrue(output.contains("foobar"), "Program output missing: $output")
        assertTrue(output.contains("jcc_gc: init:"), "GC init log missing: $output")
        assertTrue(output.contains("jcc_gc: exit:"), "GC exit log missing: $output")
        assertFalse(output.contains("jcc_gc: stub"), "Unexpected GC stub log: $output")
    }

    @Test
    fun shouldCollectUnreachableStringsInLoop() {
        // Build 100 fresh strings (each A$ + "!" concatenation allocates), keeping only one live at
        // a time (S$ is overwritten each pass). With a low threshold the collector must run
        // repeatedly; if it does, the live count at exit stays tiny even though 100 strings were
        // registered. This is what distinguishes the real runtime from the old always-leak stubs.
        val source = listOf(
            """
            DIM I AS INTEGER
            DIM A$ AS STRING
            DIM S$ AS STRING
            A$ = "item"
            I = 1
            WHILE I <= 100
                S$ = A$ + "!"
                I = I + 1
            WEND
            PRINT S$
            """
        )
        val output = compileAndRunReturningOutput(
            BASIC, source, extraArgs = arrayOf("-print-gc", "-initial-gc-threshold", "5")
        )

        // The final assignment survives and is printed correctly.
        assertTrue(output.contains("item!"), "Program output missing: $output")
        // At least one collection ran.
        assertTrue(output.contains("jcc_gc: collect:"), "No collection happened: $output")

        // The exit stats prove memory did not leak: many strings were registered, but few remain
        // live. Format: "jcc_gc: exit: registered=N collections=M freed=K live=L".
        val exitLine = output.lineSequence().first { it.contains("jcc_gc: exit:") }
        val live = Regex("""live=(\d+)""").find(exitLine)!!.groupValues[1].toInt()
        assertTrue(live < 20, "Live objects not bounded, GC likely not collecting: $exitLine")
    }
}
