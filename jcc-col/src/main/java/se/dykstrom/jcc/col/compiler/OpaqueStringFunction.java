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

package se.dykstrom.jcc.col.compiler;

import se.dykstrom.jcc.common.functions.BuiltInFunction;
import se.dykstrom.jcc.common.types.Opaque;
import se.dykstrom.jcc.common.types.Str;
import se.dykstrom.jcc.common.types.Type;

import java.util.List;

/**
 * The {@code string} conversion of an opaque type over a scalar. It formats the value exactly as
 * {@code string} formats the underlying type, so a call to it is inlined to that conversion.
 */
public class OpaqueStringFunction extends BuiltInFunction {

    public OpaqueStringFunction(final Opaque opaque) {
        super("string", List.of(opaque), Str.INSTANCE);
    }

    public Type underlying() {
        return ((Opaque) getArgTypes().getFirst()).underlying();
    }
}
