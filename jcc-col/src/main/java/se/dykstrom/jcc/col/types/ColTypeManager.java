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

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.LogicalExpression;
import se.dykstrom.jcc.common.ast.RelationalExpression;
import se.dykstrom.jcc.common.compiler.AbstractTypeManager;
import se.dykstrom.jcc.common.types.Void;
import se.dykstrom.jcc.common.types.*;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

/**
 * Manages the types in the COL language.
 *
 * @author Johan Dykstrom
 */
public class ColTypeManager extends AbstractTypeManager {

    public ColTypeManager() {
        typeToName.put(Bool.INSTANCE, "bool");
        typeToName.put(F64.INSTANCE, "f64");
        typeToName.put(I32.INSTANCE, "i32");
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
        } else if (type instanceof NamedType(String name)) {
            return name;
        } else if (type instanceof AmbiguousType(Set<Type> types)) {
            return getPossibleTypeNames(types);
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
        if ((thisType instanceof IntegerType thisIt) && (thatType instanceof IntegerType thatIt)) {
            // Smaller integer types can be assigned to larger integer types
            return thisIt.compareTo(thatIt) >= 0;
        }
        return thisType.equals(thatType);
    }

    @Override
    public Type getType(final Expression expression) {
        if (expression instanceof RelationalExpression) {
            return Bool.INSTANCE;
        } else if (expression instanceof LogicalExpression) {
            return Bool.INSTANCE;
        } else {
            return super.getType(expression);
        }
    }
}
