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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.main.Language.COL

/**
 * Compile-and-run integration tests for the COL garbage collector. COL grew its first heap type -
 * the string - and with it the collector that BASIC already uses, so the `jcc_gc_*` calls the
 * backend emits now resolve to the real mark-sweep runtime, vendored in libjcccol.
 *
 * The collector is observed the same way as for BASIC: under `-print-gc` the runtime logs its own
 * `jcc_gc: init:` / `collect:` / `exit:` lines to stdout, and the exit stats show live objects
 * staying bounded well below the number of strings created.
 *
 * @author Johan Dykstrom
 */
class ColGarbageCollectionIT : AbstractIntegrationTests() {

    @Test
    fun shouldEmitRuntimeDebugOutputUnderPrintGc() {
        val source = listOf(
            """val hello := "hello"""",
            """call println(hello + ", GC!")""",
        )
        val output = compileAndRunReturningOutput(
            COL, source, extraArgs = arrayOf("-print-gc", "-initial-gc-threshold", "100")
        )
        assertTrue(output.contains("hello, GC!"), "Program output missing: $output")
        assertTrue(output.contains("jcc_gc: init:"), "GC init log missing: $output")
        assertTrue(output.contains("jcc_gc: exit:"), "GC exit log missing: $output")
    }

    @Test
    fun shouldNotLogAnythingWithoutPrintGc() {
        // The collector still runs, it is just silent - the program's own output is all there is
        val source = listOf(
            """val hello := "hello"""",
            """call println(hello + "!")""",
        )
        val output = compileAndRunReturningOutput(COL, source)
        assertTrue(output.contains("hello!"), "Program output missing: $output")
        assertFalse(output.contains("jcc_gc:"), "Unexpected GC log: $output")
    }

    @Test
    fun shouldCollectUnreachableStringsInLoop() {
        // COL has no mutable variables, so millis() is its only terminating loop and the iteration
        // count is timing-dependent - the assertions below are therefore count-independent. The
        // concatenation sits in the loop condition, which keeps the body empty (no unused val, no
        // per-iteration output) while still allocating a fresh string every pass. One operand is a
        // val reference on purpose: two literals in a while condition would fold away at -O1.
        val source = listOf(
            """val prefix := "item"""",
            "val start := millis()",
            """while (prefix + "!") == "item!" and millis() - start < 10 do""",
            "end",
            """call println(prefix + "!")""",
        )
        val output = compileAndRunReturningOutput(
            COL, source, extraArgs = arrayOf("-print-gc", "-initial-gc-threshold", "$THRESHOLD")
        )

        // The loop terminated and the value built after it is correct.
        assertTrue(output.contains("item!"), "Program output missing: $output")
        // At least one collection ran.
        assertTrue(output.contains("jcc_gc: collect:"), "No collection happened: $output")

        // The exit stats prove memory did not leak. Format:
        // "jcc_gc: exit: registered=N collections=M freed=K live=L".
        val exitLine = output.lineSequence().first { it.contains("jcc_gc: exit:") }
        val registered = Regex("""registered=(\d+)""").find(exitLine)!!.groupValues[1].toInt()
        val freed = Regex("""freed=(\d+)""").find(exitLine)!!.groupValues[1].toInt()
        val live = Regex("""live=(\d+)""").find(exitLine)!!.groupValues[1].toInt()
        // The loop allocated far past the threshold...
        assertTrue(registered > THRESHOLD * 10, "Loop did not allocate past the threshold: $exitLine")
        // ...and nearly all of it was reclaimed while the program ran, which is the whole point:
        // one static slot is reused every iteration, so each pass drops the previous string.
        assertTrue(freed > registered / 2, "Strings were not reclaimed: $exitLine")
        // What is still live at exit is bounded by the threshold, not by the iteration count: a
        // collection runs when the live count reaches the threshold, so up to that many
        // unreachable strings can be pending when the program ends. Bounded, and independent of how
        // many iterations the timing allowed.
        assertTrue(live <= 2 * THRESHOLD, "Live objects not bounded, GC likely not collecting: $exitLine")
    }

    @Test
    fun shouldKeepShadowStackFlatAcrossBecomeChain() {
        // Every tail leaf pops its frame, including the non-become value leaf that COL's own
        // function-definition generator emits, so a deep become chain neither grows the shadow
        // stack nor leaks frames. Without the pop this still prints the right answer, which is why
        // the frame depth is checked through the collector's own accounting instead.
        val source = listOf(
            "fun count(n as i64, acc as i64) -> i64 :=",
            "    if n <= 0 then acc else become count(n - 1, acc + 1)",
            "call println(count(100000, 0))",
            """val s := "done"""",
            """call println(s + "!")""",
        )
        val output = compileAndRunReturningOutput(
            COL, source, extraArgs = arrayOf("-print-gc", "-initial-gc-threshold", "4")
        )
        assertTrue(output.contains("100000"), "Program output missing: $output")
        assertTrue(output.contains("done!"), "Program output missing: $output")
        // The string work after the chain still collects normally, which it could not do if the
        // chain had left 100000 stale frames behind.
        assertTrue(output.contains("jcc_gc: exit:"), "GC exit log missing: $output")
    }

    @Test
    fun shouldKeepEveryCallersStringAliveAcrossDeepRecursion() {
        // Each frame builds a string, roots it, and then recurses - so at the bottom every string
        // ever built is still reachable through a live frame. With a threshold of 4 a collection
        // runs at nearly every allocation, and the exit stats say freed=0: nothing was reclaimed
        // because nothing was garbage. A frame that failed to root its parameter would show up
        // twice over - as strings freed while still in use, and as corrupted output.
        val depth = 100
        val source = listOf(
            "fun chain(s as string, n as i64) -> string :=",
            """    if n == 0 then s else chain(s + "*", n - 1)""",
            """call println(chain("x", $depth) + "!")""",
        )
        val output = compileAndRunReturningOutput(
            COL, source, extraArgs = arrayOf("-print-gc", "-initial-gc-threshold", "4")
        )

        assertTrue(output.contains("x" + "*".repeat(depth) + "!"), "Program output missing or corrupt: $output")
        assertTrue(output.contains("jcc_gc: collect:"), "No collection happened: $output")
        val exitLine = output.lineSequence().first { it.contains("jcc_gc: exit:") }
        val freed = Regex("""freed=(\d+)""").find(exitLine)!!.groupValues[1].toInt()
        assertEquals(0, freed, "A reachable string was reclaimed, so a frame did not root it: $exitLine")
    }

    @Test
    fun shouldKeepShadowStackFlatAcrossStringBecomeChain() {
        // The same constant-memory guarantee as the i64 chain above, but with a string accumulator,
        // so every iteration allocates and the collector runs throughout. The accumulator keeps its
        // length, so the live set is bounded by the threshold no matter how deep the chain goes -
        // which is only true if each become pops its frame before the tail call.
        val iterations = 100000
        val source = listOf(
            "fun spin(tag as string, n as i64) -> string :=",
            """    if n == 0 then tag else become spin(tag + "", n - 1)""",
            """call println(spin("v", $iterations))""",
        )
        val output = compileAndRunReturningOutput(
            COL, source, extraArgs = arrayOf("-print-gc", "-initial-gc-threshold", "4")
        )

        assertTrue(output.contains("v"), "Program output missing: $output")
        val exitLine = output.lineSequence().first { it.contains("jcc_gc: exit:") }
        val registered = Regex("""registered=(\d+)""").find(exitLine)!!.groupValues[1].toInt()
        val live = Regex("""live=(\d+)""").find(exitLine)!!.groupValues[1].toInt()
        // Every iteration allocated...
        assertEquals(iterations, registered, "Expected one allocation per iteration: $exitLine")
        // ...and what survives is bounded by the threshold, not by the chain's depth. A become that
        // did not pop would retain a root per iteration and blow this up by four orders of magnitude.
        assertTrue(live <= 2 * 4, "Live objects grew with the chain, so a frame was not popped: $exitLine")
    }

    companion object {
        /** Initial live-object threshold used by the loop test, and the bound its assertions use. */
        private const val THRESHOLD = 100
    }
}
