/*
 * Copyright (C) 2023 Johan Dykstrom
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

package se.dykstrom.jcc.col.types;

import se.dykstrom.jcc.common.compiler.AbstractTypeManager;
import se.dykstrom.jcc.common.types.Void;
import se.dykstrom.jcc.common.types.*;

import java.util.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

/**
 * Manages the types in the COL language.
 *
 * @author Johan Dykstrom
 */
public class ColTypeManager extends AbstractTypeManager {

    public ColTypeManager() {
        typeToName.put(F64.INSTANCE, "f64");
        typeToName.put(I64.INSTANCE, "i64");
        typeToName.put(Str.INSTANCE, "string");
        typeToName.put(Void.INSTANCE, "void");
        typeToName.forEach((key, value) -> nameToType.put(value, key));
    }

    @Override
    public String getTypeName(final Type type) {
        if (typeToName.containsKey(type)) {
            return typeToName.get(type);
        } else if (type instanceof Arr array) {
            if (array == Arr.INSTANCE) {
                return "T[]";
            } else {
                return getTypeName(array.getElementType()) + getBrackets(array.getDimensions());
            }
        } else if (type instanceof Fun function) {
            return "function(" + getArgTypeNames(function.getArgTypes()) + ")->" + getTypeName(function.getReturnType());
        } else if (type instanceof NamedType namedType) {
            return namedType.name();
        } else if (type instanceof AmbiguousType at) {
            return getPossibleTypeNames(at.types());
        } else if (type == null) {
            return "unknown";
        }
        throw new IllegalArgumentException("unknown type: " + type.getClass().getSimpleName());
    }

    private String getBrackets(int dimensions) {
        return IntStream.range(0, dimensions).mapToObj(i -> "[]").collect(joining());
    }

    private String getArgTypeNames(List<Type> argTypes) {
        return argTypes.stream().map(this::getTypeName).collect(joining(", "));
    }

    private String getPossibleTypeNames(final Set<Type> possibleTypes) {
        return possibleTypes.stream()
                            .map(this::getTypeName)
                            .sorted()
                            .collect(joining(" | ", "[", "]"));
    }

    @Override
    public boolean isAssignableFrom(final Type thisType, final Type thatType) {
        if (thisType == Arr.INSTANCE && thatType instanceof Arr) {
            // All arrays are assignable to an array of the generic array type
            return true;
        }
        return thisType.equals(thatType) || thisType instanceof NumericType && thatType instanceof NumericType;
    }
}
