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
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import se.dykstrom.jcc.main.Language.BASIC

/**
 * Phase 6 (issue #63) "hardening" integration tests for the BASIC LLVM garbage collector.
 *
 * Where [BasicGarbageCollectionIT] pins the phase-4/5 correctness story (the argument-aliasing
 * fix and one collecting loop), this class stresses the collector across the whole surface of BASIC
 * constructs that either *produce* dynamic strings (concatenation, `str$`, user functions, LINE
 * INPUT) or *root* them (scalars, string arrays, function parameters, SWAP). Each test drives a real
 * program compiled against the libjccbas 2.2.0 mark-sweep runtime and asserts on both the program
 * output and the runtime's own `-print-gc` log lines.
 *
 * The runtime log formats these tests rely on (stable, emitted to stdout under `-print-gc`):
 * ```
 * jcc_gc: init: threshold=N
 * jcc_gc: collect: live X -> Y (freed Z)
 * jcc_gc: threshold: X -> Y
 * jcc_gc: exit: registered=N collections=M freed=K live=L
 * ```
 *
 * The recurring assertion pattern is "live stays bounded far below registered": if the collector
 * roots correctly it reclaims the unreachable strings, so a program that registers hundreds of them
 * finishes with only a handful live. A rooting bug shows up as either a crash/wrong output (a live
 * string was swept) or an unbounded live count (dead strings were never reclaimed).
 *
 * @author Johan Dykstrom
 */
@Tag("LLVM")
class BasicGarbageCollectionHardeningIT : AbstractIntegrationTests() {

    /** Parses the `live=L` field out of the single `jcc_gc: exit:` line. */
    private fun liveAtExit(output: String): Int {
        val exitLine = output.lineSequence().first { it.contains("jcc_gc: exit:") }
        return Regex("""live=(\d+)""").find(exitLine)!!.groupValues[1].toInt()
    }

    /** Parses the `registered=N` field out of the single `jcc_gc: exit:` line. */
    private fun registeredAtExit(output: String): Int {
        val exitLine = output.lineSequence().first { it.contains("jcc_gc: exit:") }
        return Regex("""registered=(\d+)""").find(exitLine)!!.groupValues[1].toInt()
    }

    /** Drops the runtime's `jcc_gc:` diagnostic lines, leaving only the program's own output. */
    private fun stripGcLines(output: String): String =
        output.lineSequence().filterNot { it.startsWith("jcc_gc:") }.joinToString("\n")

    /**
     * Scenario 1 — string-array stress. A 1-D string array is refilled 20 times (each pass
     * overwrites and discards the previous generation) and a 2-D string array is filled once; both
     * are kept live to the end. The array globals are registered as GC root *ranges* (design
     * decision D5): the 1-D array is one range, and the 2-D array's element region is a single range
     * spanning all elements. This proves both range kinds are scanned correctly across many
     * collections — every retained element still prints correctly — while the thousand-plus discarded
     * generations are reclaimed. The live count at exit is therefore a tiny fraction of the number
     * registered: it is bounded by the retained elements plus a fixed per-`.gc.slot` residue (a dead
     * value can linger in a static slot until `main`'s frame pops at exit — the plan's documented
     * "bounded slot retention"), and does NOT grow with the number of allocations.
     */
    @Test
    fun shouldStressStringArraysAcrossCollections() {
        val source = listOf(
            """
            DIM a$(9) AS STRING
            DIM b$(3, 3) AS STRING
            DIM i AS INTEGER
            DIM j AS INTEGER
            DIM pass AS INTEGER

            ' Refill the 1-D array many times, discarding each previous generation.
            pass = 0
            WHILE pass < 20
                i = 0
                WHILE i <= 9
                    a$(i) = "A" + ltrim$(str$(pass)) + "_" + ltrim$(str$(i))
                    i = i + 1
                WEND
                pass = pass + 1
            WEND

            ' Fill every element of the 2-D array once.
            i = 0
            WHILE i <= 3
                j = 0
                WHILE j <= 3
                    b$(i, j) = "r" + ltrim$(str$(i)) + "c" + ltrim$(str$(j))
                    j = j + 1
                WEND
                i = i + 1
            WEND

            PRINT a$(0); " "; a$(9)
            PRINT b$(0, 0); " "; b$(3, 3)
            """
        )
        val output = compileAndRunReturningOutput(
            BASIC, source, extraArgs = arrayOf("-print-gc", "-initial-gc-threshold", "5")
        )

        // Only the last generation (pass 19) survives in the 1-D array.
        assertTrue(output.contains("A19_0 A19_9"), "1-D array contents wrong: $output")
        assertTrue(output.contains("r0c0 r3c3"), "2-D array contents wrong: $output")
        assertTrue(output.contains("jcc_gc: collect:"), "No collection happened: $output")
        // Over a thousand strings are registered but only the retained elements (plus a fixed slot
        // residue) stay live: the surviving fraction is small and independent of the churn count.
        assertTrue(
            liveAtExit(output) * 4 < registeredAtExit(output),
            "Live not a small fraction of registered, memory likely leaking: ${output.lines().last { it.isNotBlank() }}"
        )
    }

    /**
     * Scenario 2 — SWAP of rooted string slots. Under slot-address rooting (design decision D1) a
     * root is the *address* of a pointer slot and the collector reads its current value at mark
     * time, so SWAP is a plain content exchange between two already-rooted slots and needs no GC
     * bookkeeping. This test swaps two string-array elements and two string scalars with a low
     * threshold so a collection runs among the allocations, then verifies the values were exchanged
     * and nothing was corrupted or swept.
     */
    @Test
    fun shouldSwapRootedStringSlots() {
        val source = listOf(
            """
            DIM arr$(3) AS STRING
            DIM p$ AS STRING
            DIM q$ AS STRING
            arr$(0) = "A" + "0"
            arr$(1) = "B" + "1"
            p$ = "P" + "p"
            q$ = "Q" + "q"
            SWAP arr$(0), arr$(1)
            SWAP p$, q$
            PRINT arr$(0); arr$(1)
            PRINT p$; q$
            """
        )
        val output = compileAndRunReturningOutput(
            BASIC, source, extraArgs = arrayOf("-print-gc", "-initial-gc-threshold", "2")
        )

        assertTrue(output.contains("B1A0"), "String array elements not swapped: $output")
        assertTrue(output.contains("QqPp"), "String scalars not swapped: $output")
    }

    /**
     * Scenario 3 — strings built inside a GOSUB subroutine. A subroutine reached by GOSUB inside a
     * loop builds a fresh string on every call and stores it into a rooted variable. GOSUB does not
     * open a new LLVM function frame (it stays within the enclosing function), so this specifically
     * checks that temporaries produced deep inside subroutine bodies are registered and rooted the
     * same as anywhere else: the last value prints correctly and the discarded generations are
     * reclaimed, keeping the live count tiny.
     */
    @Test
    fun shouldCollectStringsBuiltInGosubSubroutine() {
        val source = listOf(
            """
            DIM i AS INTEGER
            DIM s$ AS STRING
            i = 0
            WHILE i < 50
                GOSUB 100
                i = i + 1
            WEND
            PRINT s$
            END
            100 s$ = "x" + ltrim$(str$(i))
            110 RETURN
            """
        )
        val output = compileAndRunReturningOutput(
            BASIC, source, extraArgs = arrayOf("-print-gc", "-initial-gc-threshold", "5")
        )

        assertTrue(output.contains("x49"), "Wrong final subroutine result: $output")
        assertTrue(output.contains("jcc_gc: collect:"), "No collection happened: $output")
        assertTrue(liveAtExit(output) < 20, "Live objects not bounded: ${output.lines().last { it.isNotBlank() }}")
    }

    /**
     * Scenario 4 — shadow-stack frame stress. Recursive single-line `DEF FN` is not supported (a
     * function is not in scope within its own body), so frame stress is exercised with a chain of
     * nested string-returning functions (`FNc` -> `FNb` -> `FNa`) called in a long loop. Every call
     * pushes a GC frame in its prologue and pops it before returning (design decision D4); across
     * 200 iterations that is roughly 600 push/pop pairs. If `pop_frame` failed to truncate the root
     * stack back to its watermark, dead roots would accumulate and the live count would grow without
     * bound. Instead the final result is correct and live stays bounded, proving frames are balanced.
     */
    @Test
    fun shouldKeepFramesBalancedAcrossManyNestedCalls() {
        val source = listOf(
            """
            DEF FNa$(x AS STRING) = x + "a"
            DEF FNb$(x AS STRING) = FNa$(x) + "b"
            DEF FNc$(x AS STRING) = FNb$(x) + "c"
            DIM i AS INTEGER
            DIM s$ AS STRING
            i = 0
            WHILE i < 200
                s$ = FNc$("<" + ltrim$(str$(i)) + ">")
                i = i + 1
            WEND
            PRINT s$
            """
        )
        val output = compileAndRunReturningOutput(
            BASIC, source, extraArgs = arrayOf("-print-gc", "-initial-gc-threshold", "5")
        )

        assertTrue(output.contains("<199>abc"), "Wrong final nested-call result: $output")
        assertTrue(output.contains("jcc_gc: collect:"), "No collection happened: $output")
        assertTrue(liveAtExit(output) < 30, "Frames likely unbalanced, live not bounded: ${output.lines().last { it.isNotBlank() }}")
    }

    /**
     * Scenario 5 — user functions returning pointers they do not own. This is the requirement-4
     * correctness class in full: a function may return a string *literal* (rodata), a *global*
     * variable's value, or one of its *arguments*. None of these are freshly-owned heap blocks the
     * callee allocated, yet the caller keeps using the result afterwards. The collector must root
     * each so that the result prints correctly every time and the global is left intact, never
     * freeing rodata or double-freeing an aliased argument. Each argument is a fresh concatenation to
     * make the "returned an argument" path allocate.
     */
    @Test
    fun shouldHandleFunctionsReturningLiteralGlobalOrArgument() {
        val source = listOf(
            """
            DIM g$ AS STRING
            DEF FNlit$(x AS STRING) = "LIT"
            DEF FNarg$(x AS STRING) = x
            DEF FNglob$(x AS STRING) = g$
            g$ = "GLOB" + "AL"
            PRINT FNlit$("a" + "b")
            PRINT FNarg$("c" + "d")
            PRINT FNglob$("e" + "f")
            PRINT g$
            """
        )
        val output = compileAndRunReturningOutput(
            BASIC, source, extraArgs = arrayOf("-print-gc", "-initial-gc-threshold", "2")
        )

        assertOutput(listOf("LIT", "cd", "GLOBAL", "GLOBAL"), stripGcLines(output))
    }

    /**
     * Scenario 6 — LINE INPUT inside a loop. Each `LINE INPUT` allocates a fresh string (the
     * runtime's `read_line`) which is registered and stored into the rooted variable, overwriting
     * the previous line. Reading several lines with a low threshold forces a collection between
     * reads; the echo must stay correct and the live count bounded, proving the read_line result is
     * rooted like any other allocation.
     */
    @Test
    fun shouldCollectLineInputResultsInLoop() {
        val source = listOf(
            """
            DIM i AS INTEGER
            DIM s$ AS STRING
            i = 0
            WHILE i < 3
                LINE INPUT s$
                PRINT "["; s$; "]"
                i = i + 1
            WEND
            """
        )
        val output = compileAndRunReturningOutput(
            BASIC, source,
            input = listOf("first", "second", "third"),
            extraArgs = arrayOf("-print-gc", "-initial-gc-threshold", "2")
        )

        assertOutput(listOf("[first]", "[second]", "[third]"), stripGcLines(output))
        assertTrue(liveAtExit(output) < 10, "Live objects not bounded: ${output.lines().last { it.isNotBlank() }}")
    }

    /**
     * Scenario 7 — the collection threshold doubles under a sustained live population. A growing
     * string array retains every string it is given, so after each collection the surviving
     * population still exceeds half the threshold and the runtime doubles it (design decision D9).
     * The `jcc_gc: threshold: X -> Y` lines must therefore appear, and each must show an exact
     * doubling. This guards against the doubling policy silently regressing into collect-on-every-
     * allocation.
     */
    @Test
    fun shouldDoubleThresholdUnderSustainedLivePopulation() {
        val source = listOf(
            """
            DIM a$(200) AS STRING
            DIM i AS INTEGER
            i = 0
            WHILE i <= 200
                a$(i) = "v" + ltrim$(str$(i))
                i = i + 1
            WEND
            PRINT a$(0); " "; a$(200)
            """
        )
        val output = compileAndRunReturningOutput(
            BASIC, source, extraArgs = arrayOf("-print-gc", "-initial-gc-threshold", "5")
        )

        assertTrue(output.contains("v0 v200"), "Array contents wrong: $output")

        // Every "threshold: X -> Y" line must be an exact doubling, and there must be several of
        // them (the population keeps growing, so the threshold keeps chasing it).
        val doublings = Regex("""jcc_gc: threshold: (\d+) -> (\d+)""")
            .findAll(output)
            .map { it.groupValues[1].toInt() to it.groupValues[2].toInt() }
            .toList()
        assertTrue(doublings.size >= 3, "Expected several threshold growths: $output")
        doublings.forEach { (from, to) ->
            assertEquals(from * 2, to, "Threshold did not double: $from -> $to")
        }
    }

    /**
     * Scenario 7 (complement) — the threshold does *not* grow when the live population stays low. A
     * loop that overwrites a single variable keeps at most one string live, so after every
     * collection the survivor count is well under half the threshold and no doubling is warranted.
     * Seeing zero `threshold:` lines (while collections still happen) confirms the doubling is driven
     * by live population, not merely by elapsed allocations.
     */
    @Test
    fun shouldNotGrowThresholdWhenLivePopulationStaysLow() {
        val source = listOf(
            """
            DIM i AS INTEGER
            DIM base$ AS STRING
            DIM s$ AS STRING
            base$ = "item"
            i = 0
            WHILE i < 100
                s$ = base$ + "!"
                i = i + 1
            WEND
            PRINT s$
            """
        )
        val output = compileAndRunReturningOutput(
            BASIC, source, extraArgs = arrayOf("-print-gc", "-initial-gc-threshold", "5")
        )

        assertTrue(output.contains("item!"), "Program output missing: $output")
        assertTrue(output.contains("jcc_gc: collect:"), "No collection happened: $output")
        assertFalse(output.contains("jcc_gc: threshold:"), "Threshold grew despite low live population: $output")
    }

    /**
     * Scenario 8 — roots survive optimization. ADR 0003 argues that at `-O2` LLVM must keep the
     * stores to root slots ordered before the opaque `jcc_gc_*` calls, because each slot address
     * escapes into an external function and collections only happen inside such calls, so roots are
     * never elided. This test backs that claim empirically: a GC-heavy loop compiled with `-O2` must
     * still collect (the loop is not optimized away, allocations still register) and must still
     * produce the correct result with a bounded live count. If `-O2` elided a root, the retained
     * value would be swept and the output would be wrong or the program would crash.
     */
    @Test
    fun shouldKeepRootsUnderO2() {
        val source = listOf(
            """
            DIM i AS INTEGER
            DIM base$ AS STRING
            DIM s$ AS STRING
            base$ = "item"
            i = 0
            WHILE i < 100
                s$ = base$ + "!"
                i = i + 1
            WEND
            PRINT s$
            """
        )
        val output = compileAndRunReturningOutput(
            BASIC, source, extraArgs = arrayOf("-O2", "-print-gc", "-initial-gc-threshold", "5")
        )

        assertTrue(output.contains("item!"), "Wrong output under -O2, a root may have been elided: $output")
        assertTrue(output.contains("jcc_gc: collect:"), "No collection under -O2: $output")
        assertTrue(liveAtExit(output) < 10, "Live objects not bounded under -O2: ${output.lines().last { it.isNotBlank() }}")
        assertTrue(registeredAtExit(output) >= 100, "Loop was optimized away, GC not exercised: $output")
    }
}
