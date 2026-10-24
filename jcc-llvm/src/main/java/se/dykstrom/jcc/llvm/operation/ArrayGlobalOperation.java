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
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

import java.util.Collections;

/**
 * Emits the element storage of a static array as a private module-level global with an
 * aggregate {@code [length x elementType]} type. The array's own
 * {@link se.dykstrom.jcc.common.types.Arr} type only carries the dimension count and
 * element type, so the total {@code length} is computed from the declaration subscripts
 * and passed in here.
 *
 * <p>Numeric elements are {@code zeroinitializer} (0 / 0.0). String elements default to the
 * empty-string constant, so an unassigned element prints as the empty string rather than a null
 * pointer — matching scalar string variables.
 */
public record ArrayGlobalOperation(Identifier identifier, Type elementType, long length) implements LlvmOperation {

    @Override
    public String toText() {
        return "@" + identifier.getMappedName() + " = private global " +
                "[" + length + " x " + elementType.llvmName() + "] " + initializer();
    }

    private String initializer() {
        // Strings default to a pointer to the empty string, not null; other types are zero.
        if (elementType instanceof Str) {
            final var element = elementType.llvmName() + " " + elementType.llvmDefaultValue();
            return "[" + String.join(", ", Collections.nCopies((int) length, element)) + "]";
        }
        return "zeroinitializer";
    }

    @Override
    public String toString() {
        return toText();
    }
}
