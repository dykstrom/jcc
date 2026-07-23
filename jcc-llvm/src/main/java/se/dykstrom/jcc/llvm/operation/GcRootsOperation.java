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

package se.dykstrom.jcc.llvm.operation;

import se.dykstrom.jcc.llvm.code.GcRootRange;

import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

/**
 * Emits the garbage collector's global-roots table as a private module-level global:
 * <pre>
 * &#64;jcc.gc.global.roots = private global [N x { ptr, i64 }]
 *     [{ ptr, i64 } { ptr &#64;"_s$", i64 1 }, { ptr, i64 } { ptr null, i64 0 }]
 * </pre>
 * Each entry is a {@code jcc_gc_root_range_t} ({@code { ptr slots, i64 count }}): a scalar
 * string variable is one range of count 1, a string array's element region is one range whose
 * count is the number of elements. The array is null-terminated with a final {@code { null, 0 }}
 * entry, matching the contract of {@code jcc_gc_set_global_roots} in {@code jcc_gc.h}; the
 * runtime reads it (without copying) at every collection. See issue #63.
 */
public record GcRootsOperation(List<GcRootRange> ranges) implements LlvmOperation {

    /** The (unprefixed) name of the global-roots table, referenced by set_global_roots. */
    public static final String GLOBAL_ROOTS_NAME = "jcc.gc.global.roots";

    /** The LLVM type of a single range entry, {@code jcc_gc_root_range_t}. */
    private static final String ENTRY_TYPE = "{ ptr, i64 }";

    @Override
    public String toText() {
        // A null-terminated array: one entry per range, plus the terminator { ptr null, i64 0 }.
        final var entries = Stream.concat(
                ranges.stream().map(r -> entry("ptr " + r.slotName(), r.count())),
                Stream.of(entry("ptr null", 0))
        ).collect(joining(", ", "[", "]"));

        final long length = ranges.size() + 1L;
        return "@" + GLOBAL_ROOTS_NAME + " = private global " +
                "[" + length + " x " + ENTRY_TYPE + "] " + entries;
    }

    private static String entry(final String slots, final long count) {
        return ENTRY_TYPE + " { " + slots + ", i64 " + count + " }";
    }

    @Override
    public String toString() {
        return toText();
    }
}
