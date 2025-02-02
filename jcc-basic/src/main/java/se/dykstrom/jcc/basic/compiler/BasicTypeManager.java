/*
 * Copyright (C) 2016 Johan Dykstrom
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

package se.dykstrom.jcc.basic.compiler;

import se.dykstrom.jcc.common.ast.Expression;
import se.dykstrom.jcc.common.ast.RelationalExpression;
import se.dykstrom.jcc.common.compiler.AbstractTypeManager;
import se.dykstrom.jcc.common.types.*;

import java.util.*;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

/**
 * Manages the types in the Basic language.
 *
 * @author Johan Dykstrom
 */
public class BasicTypeManager extends AbstractTypeManager {

    private final Map<Character, Type> identifierTypes = new HashMap<>();

    public BasicTypeManager() {
        typeToName.put(F64.INSTANCE, "double");
        typeToName.put(I64.INSTANCE, "integer");
        typeToName.put(Str.INSTANCE, "string");
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
        }
        throw new IllegalArgumentException("unknown type: " + type.getName());
    }

    private String getBrackets(int dimensions) {
        return IntStream.range(0, dimensions).mapToObj(i -> "[]").collect(joining());
    }

    private String getArgTypeNames(List<Type> argTypes) {
        return argTypes.stream().map(this::getTypeName).collect(joining(", "));
    }

    @Override
    public boolean isAssignableFrom(final Type thisType, final Type thatType) {
        if (thatType instanceof Fun) {
            return false;
        } else if (thisType == Arr.INSTANCE && thatType instanceof Arr) {
            // All arrays are assignable to an array of the generic array type
            return true;
        }
        return thisType.equals(thatType) || thisType instanceof NumericType && thatType instanceof NumericType;
    }

    @Override
    public Type getType(final Expression expression) {
        if (expression instanceof RelationalExpression) {
            return I64.INSTANCE;
        } else {
            return super.getType(expression);
        }
    }

    /**
     * Returns true if the source type is a floating point type and the target type is an integer type.
     */
    public boolean isFloatToInt(final Type targetType, final Type sourceType) {
        return sourceType instanceof F64 && targetType instanceof I64;
    }

    /**
     * Defines identifiers starting with one of {@code letters} to be of the given type.
     *
     * @param letters A set of letters that start identifiers of the given type.
     * @param type The type to associate with the letters.
     */
    public void defineTypeByName(Set<Character> letters, Type type) {
        letters.forEach(c -> identifierTypes.put(c, type));
    }

    /**
     * Returns the type of the identifier with the given name,
     * or an empty optional if the identifier name does not imply
     * a specific type.
     */
    public Optional<Type> getTypeByName(final String name) {
        return Optional.ofNullable(identifierTypes.get(name.charAt(0)));
    }

    /**
     * Returns the optional type of the identifier with the given name,
     * using only the type specifier to determine the type. If there is no
     * type specifier, this method will return an empty optional. You cannot
     * use this method to reliably find out the identifier type, only to
     * find out what the type specifier says.
     */
    public Optional<Type> getTypeByTypeSpecifier(final String name) {
        if (name.endsWith("%")) {
            return Optional.of(I64.INSTANCE);
        } else if (name.endsWith("$")) {
            return Optional.of(Str.INSTANCE);
        } else if (name.endsWith("#")) {
            return Optional.of(F64.INSTANCE);
        } else {
            return Optional.empty();
        }
    }
}
