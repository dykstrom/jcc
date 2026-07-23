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

import se.dykstrom.jcc.common.types.Identifier;

import java.util.List;

import static java.util.stream.Collectors.joining;

/**
 * Emits the dimension-size metadata of a static array as a private constant {@code [D x i64]}
 * global, holding the (inclusive-adjusted) size of each dimension in declaration order. Used to
 * compute {@code UBOUND} inline, including for a runtime dimension argument. The global is named
 * {@code <array>_dims}.
 */
public record ArrayDimsOperation(Identifier identifier, List<Long> sizes) implements LlvmOperation {

    /** Suffix appended to the array's mapped name to form the dimension-metadata global name. */
    public static final String DIMS_SUFFIX = "_dims";

    @Override
    public String toText() {
        final var values = sizes.stream().map(s -> "i64 " + s).collect(joining(", "));
        return "@" + identifier.getMappedName() + DIMS_SUFFIX + " = private constant " +
                "[" + sizes.size() + " x i64] [" + values + "]";
    }

    @Override
    public String toString() {
        return toText();
    }
}
