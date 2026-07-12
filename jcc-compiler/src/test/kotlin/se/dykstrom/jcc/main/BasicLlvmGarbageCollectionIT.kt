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

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.main.Language.BASIC

/**
 * Compile-and-run integration tests for the BASIC LLVM garbage collector (issue #63 phase 4).
 *
 * Phase 4 hands every freshly-allocated string to the collector ([registration][RuntimeGcCodeGenerator])
 * and roots it, and deletes the old eager frees. This fixes the requirement-4 correctness class:
 * a user-defined function may return an argument it does not own, and the caller may pass a
 * transient (freshly-allocated) string as that argument. The old backend eagerly freed such a
 * transient argument right after the call, so the returned alias became a use-after-free. It is
 * now kept reachable instead.
 *
 * The runtime does not exist until phase 5, so the `jcc_gc_*` symbols resolve to the in-module
 * no-op stubs (which never collect): programs leak but are correct. These tests therefore assert
 * correctness of the output, and - under `-print-gc` - that the registration calls are really
 * wired into the linked program (the stub logs `jcc_gc: stub register`).
 *
 * @author Johan Dykstrom
 */
@Tag("LLVM")
class BasicLlvmGarbageCollectionIT : AbstractIntegrationTests() {

    @Test
    fun shouldNotFreeTransientArgumentReturnedByFunction() {
        // FNid$ returns its argument; the arguments are transient concatenations. Under the old
        // eager-free behaviour the concatenation passed to FNid$ was freed right after the call,
        // leaving the returned value (and anything built from it) dangling. Phase 4 registers and
        // roots the concatenation instead, so every result below is correct.
        val source = listOf(
            """
            DEF FNid$(x AS STRING) = x
            DEF FNcat$(x AS STRING, y AS STRING) = x + y

            PRINT FNid$("Hello, " + "GC!")
            PRINT FNcat$(FNid$("a" + "b"), FNid$("c" + "d"))
            """
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourcePath, BASIC)
        runLlvmAndAssertSuccess(
            listOf(),
            listOf(
                "Hello, GC!",
                "abcd",
            )
        )
    }

    @Test
    fun shouldEmitRegistrationCallsObservableUnderPrintGc() {
        // With -print-gc the register stub logs "jcc_gc: stub register" on every registration.
        // Seeing it proves the register calls this phase emits are compiled and linked, not just
        // present in the IR. The output still contains the program's own line.
        val source = listOf(
            """
            PRINT "foo" + "bar"
            """
        )
        val sourcePath = createSourceFile(source, BASIC)
        compileLlvmAndAssertSuccess(sourcePath, BASIC, "-print-gc", "-initial-gc-threshold", "100")

        val output = runLlvmAndReturnOutput()
        assertTrue(output.contains("foobar"), "Program output missing: $output")
        assertTrue(output.contains("jcc_gc: stub register"), "GC stub register log missing: $output")
    }
}
