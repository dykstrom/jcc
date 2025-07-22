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

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.joining;

/**
 * Represents the function type. Function types are parameterized by their argument and return types.
 * Instances of class {@code Fun} are created by calling the static factory method {@link #from(List, Type)}.
 *
 * @author Johan Dykstrom
 */
public class Fun extends AbstractType {
    
    private final List<Type> argTypes;
    private final Type returnType;

    /**
     * Creates a new function type representing a function with the given argument types and return type.
     */
    private Fun(List<Type> argTypes, Type returnType) {
        this.argTypes = argTypes;
        this.returnType = returnType;
    }

    /**
     * Returns a {@code Fun} instance that represents a function taking arguments of types {@code argTypes},
     * and returning a value of type {@code returnType}.
     */
    public static Fun from(List<Type> argTypes, Type returnType) {
        return new Fun(argTypes, returnType);
    }
    
    @Override
    public String toString() {
        return "(" + toString(argTypes) + ")->" + returnType;
    }

    private String toString(List<Type> argTypes) {
        return argTypes.stream().map(Type::toString).collect(joining(", "));
    }

    /**
     * Returns the argument types for this function type.
     */
    public List<Type> getArgTypes() {
        return argTypes;
    }
    
    /**
     * Returns the return type of this function type.
     */
    public Type getReturnType() {
        return returnType;
    }

    @Override
    public String llvmName() {
        return "ptr"; // Function references are treated as opaque pointers in LLVM
    }

    @Override
    public String getDefaultValue() {
        throw new UnsupportedOperationException("function");
    }

    @Override
    public String getFormat() {
        return "0x%llx"; // When printing a function reference, the memory address of the function will be printed
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Fun that = (Fun) o;
        return Objects.equals(argTypes, that.argTypes) && Objects.equals(returnType, that.returnType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnType, argTypes);
    }
}
