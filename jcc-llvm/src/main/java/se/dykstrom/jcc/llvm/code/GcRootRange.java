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

package se.dykstrom.jcc.llvm.code;

/**
 * A range of consecutive global pointer slots that are garbage-collector roots, mirroring the
 * {@code jcc_gc_root_range_t} struct in {@code jcc_gc.h}. A scalar string variable is a range
 * of {@link #count} 1; a string array's element region is one range spanning all its elements.
 *
 * @param slotName The fully rendered LLVM operand naming the first slot's address, e.g.
 *                 {@code @"_s$"} for a scalar or {@code @"_a$_arr"} for an array's base.
 * @param count    The number of consecutive pointer slots in the range.
 */
public record GcRootRange(String slotName, long count) { }
