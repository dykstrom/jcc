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

import se.dykstrom.jcc.common.types.Constant;
import se.dykstrom.jcc.common.types.Identifier;
import se.dykstrom.jcc.common.types.Fun;
import se.dykstrom.jcc.common.types.Type;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.emptySet;
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
    private final boolean isVarargs;
    private final List<Type> argTypes;
    private final Type returnType;
    private final Map<String, Set<String>> dependencies;
    private final Set<Constant> constants;

    /**
     * Create a new function.
     * 
     * @param name The name of the function.
     * @param isVarargs True if this is a varargs function.
     * @param argTypes The types of the formal arguments.
     * @param returnType The function return type.
     * @param dependencies The dependencies the function has on libraries and library functions.
     */
    Function(String name, boolean isVarargs, List<Type> argTypes, Type returnType, Map<String, Set<String>> dependencies) {
        this(name, isVarargs, argTypes, returnType, dependencies, emptySet());
    }

    /**
     * Create a new function.
     *
     * @param name The name of the function.
     * @param isVarargs True if this is a varargs function.
     * @param argTypes The types of the formal arguments.
     * @param returnType The function return type.
     * @param dependencies The dependencies the function has on libraries and library functions.
     * @param constants The dependencies the function has on global constants.
     */
    public Function(String name, boolean isVarargs, List<Type> argTypes, Type returnType, Map<String, Set<String>> dependencies, Set<Constant> constants) {
        this.name = name;
        this.isVarargs = isVarargs;
        this.argTypes = argTypes;
        this.returnType = returnType;
        this.dependencies = dependencies;
        this.constants = constants;
    }

    @Override
    public String toString() {
        return name + "(" + toString(argTypes) + ") : " + returnType;
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

    /**
     * Returns the mapped name of this function, that is, the name that should be used in code generation
     * to avoid any clashes with the backend assembler reserved words, and to allow function overloading.
     */
    public abstract String getMappedName();

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

    /**
     * Returns the dependencies of this function. The returned map contains entries that map a 
     * library file name to a set of library function names.
     */
    public Map<String, Set<String>> getDependencies() {
        return dependencies;
    }

    /**
     * Returns the dependencies on constants of this function.
     */
    public Set<Constant> getConstants() {
        return constants;
    }

    /**
     * Returns {@code true} if this is a varargs function.
     */
    public boolean isVarargs() {
        return isVarargs;
    }
}
