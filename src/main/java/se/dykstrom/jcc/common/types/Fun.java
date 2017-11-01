/*
 * Copyright (C) 2017 Johan Dykstrom
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

package se.dykstrom.jcc.common.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the function type. Function types are parameterized by their return type.
 * Instances of class {@code Fun} are created by calling the static factory method 
 * {@link #from(Type)}.
 *
 * @author Johan Dykstrom
 */
public class Fun extends AbstractType {

    /** Contains all function types created so far. */
    private static final Map<Type, Fun> FUNCTION_TYPES = new HashMap<>();
    
    private final Type returnType;

    /**
     * Creates a new function type representing a function with the given return type.
     */
    private Fun(Type returnType) {
        this.returnType = returnType;
    }

    /**
     * Returns a {@code Fun} instance that represents the type of a function returning a value of type {@code returnType}.
     */
    public static Fun from(Type returnType) {
        return FUNCTION_TYPES.computeIfAbsent(returnType, type -> new Fun(type));
    }
    
    @Override
    public String toString() {
        return "Fun->" + returnType;
    }

    /**
     * Returns the return type of this function type.
     */
    public Type getReturnType() {
        return returnType;
    }

    @Override
    public String getDefaultValue() {
        throw new UnsupportedOperationException("unsupported for functions");
    }

    @Override
    public String getFormat() {
        throw new UnsupportedOperationException("unsupported for functions");
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnType);
    }

    @Override
    public boolean equals(Object that) {
        return this == that;
    }
}
