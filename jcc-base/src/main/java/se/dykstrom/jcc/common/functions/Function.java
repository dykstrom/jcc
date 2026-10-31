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

package se.dykstrom.jcc.common.functions;

import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * Base class for all functions. A function is defined by its name, argument types, and return type.
 * An {@code Identifier} that identifies a function has type {@code Fun}, parameterized by the function's
 * argument and return types.
 *
 * @author Johan Dykstrom
 */
public abstract class Function {
    
    private final String name;
    private final List<Type> argTypes;
    private final Type returnType;

    /**
     * Creates a new function.
     *
     * @param name The name of the function.
     * @param argTypes The types of the formal arguments, not null.
     * @param returnType The function return type, may be null.
     */
    Function(final String name, final List<Type> argTypes, final Type returnType) {
        this.name = requireNonNull(name);
        this.argTypes = new ArrayList<>(argTypes);
        this.returnType = returnType;
    }

    @Override
    public String toString() {
        return name + "(" + toString(argTypes) + ") -> " + returnType;
    }
    
    private String toString(List<Type> types) {
        return types.stream().map(Type::toString).collect(joining(", "));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Function that = (Function) o;
        return Objects.equals(name, that.name) && Objects.equals(argTypes, that.argTypes) && Objects.equals(returnType, that.returnType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, returnType, argTypes);
    }

    public String getName() {
        return name;
    }

    public abstract String mangledName();

    /**
     * Returns an identifier that identifies this function.
     */
    public Identifier getIdentifier() {
        return new Identifier(name, Fun.from(argTypes, returnType));
    }

    /**
     * Returns the return type of this function.
     */
    public Type getReturnType() {
        return returnType;
    }

    /**
     * Returns the types of the arguments of this function.
     */
    public List<Type> getArgTypes() {
        return argTypes;
    }
}
