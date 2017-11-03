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

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.Map;
import java.util.Set;

import se.dykstrom.jcc.common.types.Type;

/**
 * Base class for all functions.
 *
 * @author Johan Dykstrom
 */
public abstract class Function {
    
    private final String name;
    private final Type returnType;
    private final List<Type> argTypes;
    private final Map<String, Set<String>> dependencies;

    /**
     * Create a new function.
     * 
     * @param name The name of the function.
     * @param returnType The function return type.
     * @param argTypes The types of the formal arguments.
     * @param dependencies The dependencies the function has on libraries and library functions.
     */
    Function(String name, Type returnType, List<Type> argTypes, Map<String, Set<String>> dependencies) {
        this.name = name;
        this.returnType = returnType;
        this.argTypes = argTypes;
        this.dependencies = dependencies;
    }

    @Override
    public String toString() {
        return name + "(" + toString(argTypes) + ") : " + returnType;
    }
    
    private String toString(List<Type> types) {
        return types.stream().map(Type::toString).collect(joining(", "));
    }

    public String getName() {
        return name;
    }

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
}
